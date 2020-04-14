/*
 * NextBill Android client application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.client.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class SqlBuilder {

    private String resultString = "";
    private List<String> values = new ArrayList<>();
    private boolean whereStarted = false;
    private boolean hasFirstTableEntry = false;

    public SqlBuilder(String... tableNames) {

        String commaSeparatedTables = "";
        for (int i = 0; i < tableNames.length; i++) {
            if (i == 0){
                commaSeparatedTables = tableNames[i];
            }else{
                commaSeparatedTables = commaSeparatedTables + ", " + tableNames[i];
            }
        }

        this.resultString = "SELECT distinct * FROM " + commaSeparatedTables + " ";
    }

    public SqlBuilder(String selectString, Map<String, String> tableNameAndAlias) {

        String commaSeparatedTables = "";
        boolean isFirstEntry = true;
        for (String key : tableNameAndAlias.keySet()) {
            if (isFirstEntry){
                commaSeparatedTables = key +" as " + tableNameAndAlias.get(key);
                isFirstEntry = false;
            }else{
                commaSeparatedTables = commaSeparatedTables + ", " + key + " as " + tableNameAndAlias.get(key);
            }
        }

        this.resultString = selectString + " FROM " + commaSeparatedTables + " ";
    }

    public SqlBuilder(String tableName, String alias) {
        this.resultString = "SELECT distinct * FROM " + tableName + " as " + alias;
    }

    public SqlBuilder(Map<String, String> tableNameAndAlias) {

        String commaSeparatedTables = "";
        boolean isFirstEntry = true;
        for (String key : tableNameAndAlias.keySet()) {
            if (isFirstEntry){
                commaSeparatedTables = key +" as " + tableNameAndAlias.get(key);
                isFirstEntry = false;
            }else{
                commaSeparatedTables = commaSeparatedTables + ", " + key + " as " + tableNameAndAlias.get(key);
            }
        }

        this.resultString = "SELECT distinct * FROM " + commaSeparatedTables + " ";
    }

    public SqlBuilder addTable(String tableName, String alias){
        this.resultString = this.resultString + ", " + tableName + " as " + alias;

        return this;
    }

    private String where(){
        if (!whereStarted){
            whereStarted = true;
            return " where ";
        }
        return "";
    }

    public SqlBuilder startBracket(){
        resultString =  resultString + where() + " ( ";
        return this;
    }

    public SqlBuilder endBracket(){
        resultString =  resultString + " ) ";
        return this;
    }

    public SqlBuilder and(){
        resultString =  resultString + " and ";
        return this;
    }

    public SqlBuilder or(){
        resultString =  resultString + " or ";
        return this;
    }

    public SqlBuilder isEqualFields(String fieldName1, String fieldName2){
        resultString = resultString + where() + fieldName1 + " = " + fieldName2;
        return this;
    }

    public SqlBuilder isEqualFields(String aliasFieldName1, String fieldName1, String aliasFieldName2, String fieldName2){
        resultString = resultString + where() + aliasFieldName1 + "." + fieldName1 + " = " + aliasFieldName2 + "." + fieldName2;
        return this;
    }

    public SqlBuilder isEqual(String fieldName, String value){
        resultString = resultString + where() + fieldName + "=?";
        values.add(value);
        return this;
    }

    public SqlBuilder isIn(String fieldName, List<String> values){
        resultString = resultString + where() + fieldName + " in (";

        String valuesString = "";
        for (int i = 0; i < values.size(); i++){
            if (i == 0){
                valuesString = valuesString + " ?";
            }else{
                valuesString = valuesString + ", ?";
            }
        }

        resultString = resultString + valuesString + ")";

        this.values.addAll(values);
        return this;
    }

    public SqlBuilder isNotIn(String fieldName, List<String> values){
        resultString = resultString + where() + fieldName + " not in (";

        String valuesString = "";
        for (int i = 0; i < values.size(); i++){
            if (i == 0){
                valuesString = valuesString + " ?";
            }else{
                valuesString = valuesString + ", ?";
            }
        }

        resultString = resultString + valuesString + ")";

        this.values.addAll(values);
        return this;
    }

    public SqlBuilder isIn(String alias, String fieldName, List<String> values){
        resultString = resultString + where() + alias + "." + fieldName + " in (";

        String valuesString = "";
        for (int i = 0; i < values.size(); i++){
            if (i == 0){
                valuesString = valuesString + " ?";
            }else{
                valuesString = valuesString + ", ?";
            }
        }

        resultString = resultString + valuesString + ")";

        this.values.addAll(values);
        return this;
    }

    public SqlBuilder isEqual(String alias, String fieldName, String value){
        resultString = resultString + where() + alias + "." + fieldName + "=?";
        values.add(value);
        return this;
    }

    public SqlBuilder isNotEqual(String fieldName, String value){
        resultString = resultString + where() + "NOT " + fieldName + "=?";
        values.add(value);
        return this;
    }

    public SqlBuilder isNotEqual(String alias, String fieldName, String value){
        resultString = resultString + where() + "NOT " +  alias + "." + fieldName + "=?";
        values.add(value);
        return this;
    }

    public SqlBuilder isNotEqualFields(String fieldName1, String fieldName2){
        resultString = resultString + where() + "NOT " + fieldName1 + " = " + fieldName2;
        return this;
    }

    public SqlBuilder isNotEqualFields(String aliasFieldName1, String fieldName1, String aliasFieldName2, String fieldName2){
        resultString = resultString + where() + "NOT " + aliasFieldName1 + "." + fieldName1 + " = " + aliasFieldName2 + "." + fieldName2;
        return this;
    }

    public SqlBuilder isNull(String fieldName){
        resultString = resultString + where() + fieldName + " IS NULL";
        return this;
    }

    public SqlBuilder isAfterThisDate(String alias, String fieldName, Date customDate){
        resultString = resultString + where() + alias + "." + fieldName + " >= " + customDate.getTime();
        return this;
    }

    public SqlBuilder isBeforeThisDate(String alias, String fieldName, Date customDate){
        resultString = resultString + where() + alias + "." + fieldName + " <= " + customDate.getTime();
        return this;
    }

    public SqlBuilder isNull(String alias, String fieldName){
        resultString = resultString + where() + alias + "." + fieldName + " IS NULL";
        return this;
    }

    public SqlBuilder isNotNull(String fieldName){
        resultString = resultString + where() + fieldName + " NOT NULL";
        return this;
    }

    public SqlBuilder isNotNull(String alias, String fieldName){
        resultString = resultString + where() + alias + "." + fieldName + " NOT NULL";
        return this;
    }

    public SqlBuilder orderBy(String alias, String fieldName){
        resultString = resultString + " order by " + alias + "." + fieldName;
        return this;
    }

    public SqlBuilder orderBy(String fieldName){
        resultString = resultString + " order by " + fieldName;
        return this;
    }

    public String getSqlString(){
        return resultString + ";";
    }

    public List<String> getValuesAsList(){
        return values;
    }

    public String[] getValues(){
        String[] returnValues = new String[values.size()];
        for (int i = 0; i < values.size(); i++){
            returnValues[i] = values.get(i);
        }
        return returnValues;
    }

    public SqlBuilder copy(){
        SqlBuilder sqlBuilder = new SqlBuilder();
        sqlBuilder.resultString = this.resultString;
        sqlBuilder.values.addAll(this.values);
        sqlBuilder.whereStarted = this.whereStarted;
        sqlBuilder.hasFirstTableEntry = this.hasFirstTableEntry;

        return sqlBuilder;
    }
}
