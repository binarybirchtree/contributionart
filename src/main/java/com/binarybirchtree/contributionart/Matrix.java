package com.binarybirchtree.contributionart;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableBiMap;

///
/// Provides a representation of a GitHub contribution graph,
/// which shows one year of contributions in a grid with 53 columns and 7 rows.
/// Note that the first and last columns may not be completely shown,
/// depending on the current date used when generating the graph.
///
public class Matrix implements Iterable<Matrix.Value> {
  private static final Logger LOGGER = Logger.getLogger(Matrix.class.getName());

  public static final int ROWS = 7;
  public static final int COLUMNS = 53;
  public static final int AREA = ROWS * COLUMNS;

  public class FileFormatException extends Exception {
    public FileFormatException (String message) {
      super(message);
    }
  }

  public enum Value {
    EMPTY(0), LIGHT(1), MEDIUM(2), DARK(3);

    private int weight;

    Value (int weight) {
      this.weight = weight;
    }

    private static final ImmutableBiMap<Character, Value> definitions = ImmutableBiMap.of(
      ' ', Value.EMPTY,
      '.', Value.LIGHT,
      ':', Value.MEDIUM,
      '!', Value.DARK
    );

    ///
    /// @return Character representation of the enum value.
    ///
    public char character () {
      Character character = definitions.inverse().get(this);
      assert character != null;
      return character;
    }

    ///
    /// @param[in] character Character to parse.
    /// @return Value corresponding to the specified character if one exists; null otherwise.
    ///
    public static Value parse (char character) {
      return definitions.get(character);
    }

    ///
    /// @return Weight attributable to the enum value.
    ///
    public int weight () {
      return this.weight;
    }
  }

  private Value[][] values = new Value[COLUMNS][ROWS];

  ///
  /// Initializes the matrix from a definition file.
  ///
  /// @param[in] file Path to definition file.
  ///
  public Matrix (Path file) throws IOException, FileFormatException {
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      for (int row = 0; row < ROWS; ++row) {
        String line = reader.readLine();
        if (line == null || line.length() < COLUMNS) {
          throw new FileFormatException(String.format("Invalid definition file: '%s'", file));
        }

        for (int col = 0; col < COLUMNS; ++col) {
          char character = line.charAt(col);
          Value value = Value.parse(line.charAt(col));
          if (value == null) {
            throw new FileFormatException(String.format("Invalid character encountered at line %d, column %d of '%s': '%s'", row, col, file, character));
          }
          else {
            values[col][row] = value;
          }
        }
      }
    }

    LOGGER.info(String.format("Initialized matrix from definition file '%s'.", file));
  }

  @Override
  public Iterator<Value> iterator () {
    return new Iterator<Value>() {
      private int row = 0;
      private int col = 0;

      @Override
      public boolean hasNext () {
        return col < COLUMNS;
      }

      @Override
      public Value next () {
        Value value = values[col][row];
        if (row == ROWS - 1) {
          row = 0;
          ++col;
        }
        else {
          ++row;
        }
        return value;
      }
    };
  }

  @Override
  public String toString () {
    StringBuilder builder = new StringBuilder();
    for (int row = 0; row < ROWS; ++row) {
      for (int col = 0; col < COLUMNS; ++col) {
        builder.append(values[col][row].character());
      }
      builder.append(System.lineSeparator());
    }
    return builder.toString();
  }
}
