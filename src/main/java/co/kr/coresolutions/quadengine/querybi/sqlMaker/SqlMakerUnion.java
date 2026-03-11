package co.kr.coresolutions.quadengine.querybi.sqlMaker;

import lombok.Getter;
import lombok.Setter;
import co.kr.coresolutions.quadengine.querybi.sqlMaker.SqlMaker;

@Getter
@Setter
public class SqlMakerUnion extends SqlMaker {

    private String sql;
    private boolean startF;
    private boolean lastF;

    public SqlMakerUnion(String fieldName, int seq, String sql, boolean startF, boolean lastF) {
        super(seq, fieldName);
        this.sql = sql;
        this.startF = startF;
        this.lastF = lastF;
    }

    public String makeSql() {
        if (startF)
            return startSql();
        if (!lastF)
            return middleSql();
        return endSql();
    }

    public String startSql() {
        if (startF && lastF)
            return sql;

        return "WITH %s AS ( %s )".formatted(tableName2(), sql);
    }

    public String middleSql() {
        return """
                , %s AS (
                SELECT %s FROM %s
                INNER JOIN (%s) %s
                  ON %s = %s
                )
                """.formatted(tableName2(), fieldName1(), tableName1(), sql, tableName2(), fieldName1(), fieldName2());
    }

    public String endSql() {
        return """
                SELECT %s FROM %s
                INNER JOIN (%s) %s
                  ON %s = %s
                """.formatted(fieldName1(), tableName1(), sql, tableName2(), fieldName1(), fieldName2());
    }
}
