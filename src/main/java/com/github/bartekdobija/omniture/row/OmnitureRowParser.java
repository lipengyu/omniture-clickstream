package com.github.bartekdobija.omniture.row;

import com.github.bartekdobija.omniture.metadata.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

public class OmnitureRowParser implements RowParser {

  public static final char DEFAULT_ARRAY_SEPARATOR = ',';
  public static final char DEFAULT_COLUMN_SEPARATOR = '\t';

  private Column[] columns;
  private LookupTableIndex lookupTableIndex;
  private char arraySeparator;
  private char separator;
  private Row rowTemplate;
  private Timestamp timestampTemplate;

  private long parsedCount = 0;
  private long emptyRowCount = 0;
  private long exceptionCount = 0;

  protected OmnitureRowParser(
      Column[] cols,
      LookupTableIndex index,
      char columnSeparator,
      char arrSeparator) throws RowParserException {
    columns = cols;
    lookupTableIndex = index;
    setSeparator(columnSeparator);
    setArraySeparator(arrSeparator);
    rowTemplate = new Row(cols.length);
    timestampTemplate = new Timestamp(0);
  }

  public static OmnitureRowParser newInstance() throws RowParserException {
    return new OmnitureRowParser(
        null, null, DEFAULT_COLUMN_SEPARATOR, DEFAULT_ARRAY_SEPARATOR);
  }

  public static OmnitureRowParser netInstance(OmnitureMetadata meta)
      throws RowParserException {
    return newInstance(meta, DEFAULT_COLUMN_SEPARATOR, DEFAULT_ARRAY_SEPARATOR);
  }

  public static OmnitureRowParser newInstance(
      OmnitureMetadata meta,
      char separator,
      char arraySeparator)
      throws RowParserException {

    try {
      List<Column> cols = meta.getHeader().getColumns();
      return newInstance(
          cols.toArray(new Column[cols.size()]),
          meta.getLookupTable().getIndex(),
          separator,
          arraySeparator
      );
    } catch (MetadataException ex) {
      throw new RowParserException(ex);
    }
  }

  public static OmnitureRowParser newInstance(Column[] cols,
                                              LookupTableIndex index)
      throws RowParserException {
    return newInstance(
        cols, index, DEFAULT_COLUMN_SEPARATOR, DEFAULT_ARRAY_SEPARATOR);
  }

  public static OmnitureRowParser newInstance(List<Column> cols,
                                              LookupTableIndex index)
      throws RowParserException {
    return newInstance(
        cols.toArray(new Column[cols.size()]),
        index,
        DEFAULT_COLUMN_SEPARATOR,
        DEFAULT_ARRAY_SEPARATOR
    );
  }

  public static OmnitureRowParser newInstance(
      Header header,
      LookupTableIndex index) throws RowParserException {
    try {
      List<Column> c = header.getColumns();
      return newInstance(
          c.toArray(new Column[c.size()]),
          index,
          DEFAULT_COLUMN_SEPARATOR,
          DEFAULT_ARRAY_SEPARATOR
      );
    } catch (MetadataException e) {
      throw new RowParserException(e);
    }
  }

  public static OmnitureRowParser newInstance(
      Column[] cols,
      LookupTableIndex index,
      char colSeparator,
      char arrSeparator) throws RowParserException {
    return new OmnitureRowParser(cols, index, colSeparator, arrSeparator);
  }

  @Override
  public Row parse(String line) {
    if (line == null || line.isEmpty()) {
      emptyRowCount++;
      return null;
    }
    return parse(line.toCharArray());
  }

  @Override
  public Row parse(char[] line) {
    if (line == null || line.length == 0) {
      emptyRowCount++;
      return null;
    }

    Row row;
    try {
      row = (Row) rowTemplate.clone();
    } catch (CloneNotSupportedException e) {
      exceptionCount++;
      return null;
    }

    for (int i = 0, col = 0, offset = 0; i < line.length; i++) {
      if (line[i] == separator) {
        row.add(parseSubset(line, offset, i, col++));
        offset = i + 1;
      } else if (i == line.length - 1) {
        row.add(parseSubset(line, offset, i + 1, col));
      }
    }
    parsedCount++;
    return row;
  }

  @Override
  public RowParserStats getRowParserStats() {
    return new RowParserStats(parsedCount, emptyRowCount, exceptionCount);
  }

  private Object parseSubset(char[] line, int start, int end, int colIndex) {
    String group = columns[colIndex].getLookupGroup();
    ColumnType type = columns[colIndex].getType();

    if (type == ColumnType.STRING) {
      if (start == end) {
        return null;
      }
      String buff = String.valueOf(Arrays.copyOfRange(line, start, end));
      return group != null
          ? lookupTableIndex.getGroupValue(group, buff, buff)
          : buff;
    } else if (type == ColumnType.INT) {
      return asInteger(line, start, end);
    } else if (type == ColumnType.DOUBLE) {
      return asDouble(line, start, end);
    } else if (type == ColumnType.FLOAT) {
      return asFloat(line, start, end);
    } else if (type == ColumnType.STRING_ARRAY) {
      return asStrings(line, start, end, group);
    } else if (type == ColumnType.TIMESTAMP) {
      return asTimestamp(line, start, end);
    } else if (type == ColumnType.LONG) {
      return asLong(line, start, end);
    }

    return null;
  }

  private Integer asInteger(char[] line, int start, int end) {
    if (start == end) {
      return null;
    }

    int result = 0;
    for (int i = start; i < end; i++) {
      int digit = (int) line[i] - (int) '0';
      if ((digit < 0) || (digit > 9)) {
        return null;
      }
      result = (result * 10) + digit;
    }
    return result;
  }

  private Timestamp asTimestamp(char[] line, int start, int end) {
    Long l;
    if (start == end || (l = asLong(line, start, end)) == null) {
      return null;
    }

    Timestamp ts = (Timestamp) timestampTemplate.clone();
    ts.setTime(l * 1000);
    return ts;
  }

  private Long asLong(char[] line, int start, int end) {
    if (start == end) {
      return null;
    }

    long result = 0;
    for (int i = start; i < end; i++) {
      long digit = (long) line[i] - (long) '0';
      if ((digit < 0) || (digit > 9)) {
        return null;
      }
      result = (result * 10) + digit;
    }
    return result;
  }

  private Float asFloat(char[] data, int start, int end) {
    Double d = asDouble(data, start, end);
    return d != null ? d.floatValue() : null;
  }

  private Double asDouble(char[] data, int start, int end) {
    if (start == end) {
      return null;
    }

    double result = 0;
    double div = 0;
    for (int i = start; i< end; i++) {
      double digit = (double) data[i] - (double) '0';
      if (data[i] == '.') {
        div = 1;
        continue;
      } else if ((digit < 0) || (digit > 9)) {
        return null;
      }
      result = (result * 10) + digit;
      if (div > 0) div *= 10;
    }

    return result / ((div<1) ? 1 : div);
  }

  private String[] asStrings(char[] data, int start, int end, String group) {

    if (start == end) {
      return null;
    }

    int[] sepPos = new int[end - start];
    int i, arrSize;
    for (i = start, arrSize = 0; i < end; i++) {
      if (data[i] == arraySeparator) {
        sepPos[arrSize++] = i;
      }
    }
    sepPos[arrSize++] = end;

    String[] result = new String[arrSize];
    int es = start;
    for (i = 0; i < arrSize; i++) {
      String s = String.valueOf(Arrays.copyOfRange(data, es, sepPos[i]));
      result[i] =
          group != null
          ? lookupTableIndex.getGroupValue(group, s, s)
          : s;
      es = sepPos[i] + 1;
    }

    return result;
  }

  public Column[] getColumns() {
    return columns;
  }

  public void setColumns(Column[] columns) {
    this.columns = columns;
  }

  public LookupTableIndex getLookupTableIndex() {
    return lookupTableIndex;
  }

  public void setLookupTableIndex(LookupTableIndex lookupTableIndex) {
    this.lookupTableIndex = lookupTableIndex;
  }

  public char getArraySeparator() {
    return arraySeparator;
  }

  public void setArraySeparator(char arraySeparator) {
    this.arraySeparator = arraySeparator;
  }

  public char getSeparator() {
    return separator;
  }

  public void setSeparator(char separator) {
    this.separator = separator;
  }

  @Override
  public String toString() {
    return "OmnitureRowParser{" +
        "columns=" + Arrays.toString(columns) +
        ", lookupTableIndex=" + lookupTableIndex +
        ", arraySeparator=" + arraySeparator +
        ", separator=" + separator +
        '}';
  }
}
