package co.kr.coresolutions.quadengine.query.executor;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.query.service.Constants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.InterruptException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static co.kr.coresolutions.quadengine.query.executor.RunCommands.commandRandomFlag;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunCommandsInternal {

	@Value("${app.command.shutdown-minutes}")
	private long commandShutdownMinutes;

	private final Constants constants;
	private final RunCommands runCommands;
	private ByteClassLoader clazzLoader;

	@PostConstruct
	public void init() {
		String startPackageName = "cmsbatch";
		File jarPath = new File(constants.getCommandDir() + "jar");
		File clazzPath = new File(constants.getCommandDir() + "bin" + File.separator + startPackageName);

		// jar File Load
		File[] jarFiles = jarPath.listFiles();
		if (jarFiles != null) {
			ArrayList<URL> urls = new ArrayList<URL>();
			urls = loadJar(jarFiles, urls);
			URLClassLoader urlClazzLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
					this.getClass().getClassLoader());
			clazzLoader = new ByteClassLoader(urlClazzLoader);
		}

		// class 파일 로드
		File[] files = clazzPath.listFiles();
		if (files != null) {
			loadClass(clazzLoader, startPackageName, files);
		}
	}

	public String runCommand(String commandAfterKeyReplace, String commandId) {
		final String[] executionResult = { "" };
		final boolean[] commandDurationExceed = { false };

		String randomID = commandId.concat(UUID.randomUUID().toString());

		String clazzName = "";
		String packageName = "";
		Thread commandDurationThread = null;

		try {
			/* command에서 실행 명령 검색 */
			String clazzNameFind = commandAfterKeyReplace;
			Pattern pattern = Pattern.compile(" ([a-zA-Z]+\\.)+([a-zA-Z1-9]+){1} ");
			Matcher matcher = pattern.matcher(clazzNameFind);

			if (matcher.find()) {
				clazzNameFind = matcher.group().substring(1, matcher.group().length() - 1);
				int pos = clazzNameFind.lastIndexOf(".");
				packageName = clazzNameFind.substring(0, pos);
				clazzName = clazzNameFind.substring(pos + 1);
			} else {
				return "Error : className not found";
			}

			/* find paramKey */
			String paramKeyReplace = commandAfterKeyReplace;
			// Pattern patternDetail = Pattern.compile("(\"[^\"]*)\"");
			Pattern patternDetail = Pattern.compile("(\"[\\s\\S]*);");
			Matcher matcherDetail = patternDetail.matcher(commandAfterKeyReplace);

			if (matcherDetail.find()) {
				paramKeyReplace = matcherDetail.group().substring(1, matcherDetail.group().length() - 1);
			}

			// commandId
			paramKeyReplace = paramKeyReplace + ";" + System.lineSeparator() + "COMMAND_ID:" + commandId + ";";

			Object[] args = new Object[1];
			args[0] = new String[] { paramKeyReplace };

			Class<?> classToLoad = Class.forName(packageName + "." + clazzName, true, clazzLoader);
			final Object[] instance = { classToLoad.getConstructor(String[].class).newInstance(args) };
			Method method = classToLoad.getMethod("runNode");
			Method closeMethod = classToLoad.getMethod("close");

			final CompletableFuture<?>[] threadKillInternal = new CompletableFuture[1];
			try {
				threadKillInternal[0] = CompletableFuture.runAsync(() -> {
					try {
						Object ret = method.invoke(instance[0]);

						if (ret != null) {
							executionResult[0] = (String) ret;
						}
					} catch (Exception e) {
						log.error("exception, ", e);
						executionResult[0] = "command Error";
					}
				});
			} catch (InterruptException e) {
				log.error("exception, ", e);
				Thread.currentThread().interrupt();
			}

			runCommands.saveCacheCommandProcess(commandId, threadKillInternal[0]);
			commandRandomFlag.add(randomID);

			if (commandShutdownMinutes > 0) {
				commandDurationThread = new Thread(() -> {
					try {
						Thread.sleep(commandShutdownMinutes * 60000L);
						threadKillInternal[0].cancel(true);
						try {
							closeMethod.invoke(instance[0]);
							log.info("Method : {} , Message : {}", commandId, "command-execution-time is exceed \n");
						} catch (Exception e2) {
							log.error("Method : {} , Message : {}", "Error",
									"command-execution-time method : " + e2.toString() + "\n", e2);
						}
						if (commandRandomFlag.contains(randomID)) {
							commandRandomFlag.remove(randomID);
							commandDurationExceed[0] = true;
							RunCommands.removeCacheCommandProcess(commandId);
						}
					} catch (InterruptedException e) {
						return;
					}
				});
				commandDurationThread.setDaemon(true);
				commandDurationThread.start();
			}

			try {
				threadKillInternal[0].get();
			} catch (CancellationException e) {
				log.info("Method : {} , Message : {}", "INFO", "after threadKillInternal[0].get() is Cancelled!");
				try {
					closeMethod.invoke(instance[0]);
				} catch (Exception e2) {
					log.error("Method : {} , Message : {}", "Error", "closeMethod invoke - 1 : " + e2.toString() + "\n",
							e2);
				}
			} catch (Exception e) {
				threadKillInternal[0].cancel(true);
				try {
					closeMethod.invoke(instance[0]);
				} catch (Exception e2) {
					log.error("Method : {} , Message : {}", "Error", "closeMethod invoke - 2 : " + e2.toString() + "\n",
							e2);
				}
				log.error("exception", e);
			}
			if (threadKillInternal[0].isCancelled()) {
				if (executionResult[0] == null || executionResult[0].isEmpty()) {
					executionResult[0] = "process cancelled";
				}
			} else if (threadKillInternal[0].isDone()) {
				if (executionResult[0] == null || executionResult[0].isEmpty()) {
					executionResult[0] = "no result";
				}
			} else {
				threadKillInternal[0].cancel(true);
				try {
					closeMethod.invoke(instance[0]);
				} catch (Exception e2) {
					log.error("Method : {} , Message : {}", "Error", "closeMethod invoke - " + e2.toString(), e2);
				} finally {
					Thread threadMain = Thread.currentThread();
					Thread threadInterrupt = new Thread(() -> {
						try {
							Thread.sleep(1000);
							threadMain.interrupt();
						} catch (InterruptException | InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					});
					threadInterrupt.setDaemon(true);
					threadInterrupt.start();
				}

				if (RunCommands.getCacheCommandProcess(commandId) == null) {
					log.info("Method : {} , Message : {}", "POST",
							"Error RunCommand NULL, commandId\t" + commandId + "\n");
					throw new CommonException(ErrorCode.COMMAND_NOT_VALID);
				} else {
					log.info("Method : {} , Message : {}", "POST",
							"Error Command is terminated by request, commandId\t" + commandId + "\n");
					throw new CommonException(ErrorCode.COMMAND_CANCELED);
				}
			}
		} catch (Exception e) {
			log.error("Method : {} , Message : {}", "Error", "Main Try Exception e :" + e.toString(), e);
			// e.printStackTrace();
			if (commandRandomFlag.contains(randomID)) {
				commandRandomFlag.remove(randomID);
				RunCommands.removeCacheCommandProcess(commandId);
				executionResult[0] = "command Error";
			}
		} finally {
			if (commandDurationThread != null && commandDurationThread.isAlive()) {
				commandDurationThread.interrupt();
			}
		}
		commandRandomFlag.remove(randomID);
		return executionResult[0];
	}

	private ArrayList<URL> loadJar(File[] files, ArrayList<URL> urls) {
		String fileExt = "jar";

		for (File f : files) {
			String fileName = f.getName();
			int pos = fileName.lastIndexOf(".");
			String ext = fileName.substring(pos + 1);
			if (f.isFile() && fileExt.equals(ext)) {
				try {
					urls.add(f.toURI().toURL());
				} catch (MalformedURLException e) {
					log.error("exception, ", e);
				}
			} else if (f.isDirectory()) {
				// 하위폴더의 jar 검색
				String directoryName = f.getName();

				Pattern patternDetail = Pattern.compile("^[a-zA-Z]*$"); // 영문의 폴더만 검색 백업폴더가 존재하여 제외함.
				Matcher matcherDetail = patternDetail.matcher(directoryName);

				if (matcherDetail.find()) {
					File[] jFiles = f.listFiles();
					if (null != jFiles && jFiles.length > 0) {
						urls = loadJar(jFiles, urls);
					}
				}
			}
		}
		return urls;
	}

	private void loadClass(ByteClassLoader clazzLoader, String packageName, File[] files) {
		String fileExt = "class";

		for (File f : files) {
			String fileName = f.getName();
			int pos = fileName.lastIndexOf(".");
			String ext = fileName.substring(pos + 1);
			if (f.isFile() && fileExt.equals(ext)) {
				String clazzName = fileName.substring(0, pos);
				if ((clazzName + "." + fileExt).equals(f.getName())) {
					try {
						clazzLoader.loadClass(packageName + "." + clazzName);
					} catch (Exception e) {
						log.error("exception, ", e);
						return;
					}
				}
			} else if (f.isDirectory()) {
				// Search class from Sub Directory
				String directoryName = f.getName();

				Pattern patternDetail = Pattern.compile("^[a-zA-Z]*$"); // 영문의 폴더만 검색 백업폴더가 존재하여 제외함.
				Matcher matcherDetail = patternDetail.matcher(directoryName);

				if (matcherDetail.find()) {
					String cPackageName = packageName + "." + f.getName();
					File[] cFiles = f.listFiles();
					if (null != cFiles && cFiles.length > 0) {
						loadClass(clazzLoader, cPackageName, cFiles);
					}
				}
			}
		}
	}

	protected class ByteClassLoader extends ClassLoader {
		public ByteClassLoader(ClassLoader classLoader) {
			super(classLoader);
		}

		public byte[] findClassBytes(String name) {
			FileInputStream inFile = null;
			try {
				String fileExt = "class";
				String replaceName = name.replace(".", File.separator);
				String pathName = constants.getCommandDir() + "bin" + File.separator + replaceName + "." + fileExt;
				inFile = new FileInputStream(pathName);
				byte[] classBytes = new byte[inFile.available()];
				inFile.read(classBytes);
				return classBytes;
			} catch (IOException e) {
				log.error("exception, ", e);
				return null;
			} finally {
				try {
					inFile.close();
				} catch (IOException e) {
					log.error("exception, ", e);
				}
			}
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] classBytes = findClassBytes(name);
			if (classBytes == null) {
				throw new ClassNotFoundException();
			} else {
				return defineClass(name, classBytes, 0, classBytes.length);
			}
		}
	}
}
