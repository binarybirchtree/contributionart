package com.binarybirchtree.contributionart;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.Assert;
import org.junit.Test;

public class MatrixTest extends BaseTest {
  @Test
  public void loads_definition () throws IOException, Matrix.FileFormatException {
    Matrix matrix = new Matrix(file);
    Assert.assertEquals(definition, matrix.toString());
  }

  @Test
  public void empty_file () throws IOException, Matrix.FileFormatException {
    exception.expect(Matrix.FileFormatException.class);
    new Matrix(folder.newFile().toPath());
  }

  @Test
  public void invalid_characters () throws IOException, Matrix.FileFormatException {
    Path file = folder.newFile().toPath();
    try (BufferedWriter writer = Files.newBufferedWriter(file)) {
      writer.write(definition.replace('.', ','));
    }
    exception.expect(Matrix.FileFormatException.class);
    new Matrix(folder.newFile().toPath());
  }

  @Test
  public void partial_definition () throws IOException, Matrix.FileFormatException {
    Path file = folder.newFile().toPath();
    try (BufferedWriter writer = Files.newBufferedWriter(file)) {
      writer.write(definition.substring(definition.length() / 2));
    }
    exception.expect(Matrix.FileFormatException.class);
    new Matrix(folder.newFile().toPath());
  }

  @Test
  public void extra_characters () throws IOException, Matrix.FileFormatException {
    Matrix matrix = new Matrix(file);
    try (BufferedWriter writer = Files.newBufferedWriter(file, Charset.defaultCharset(), StandardOpenOption.APPEND)) {
      writer.write(definition.replace('!', '?'));
    }
    Assert.assertEquals(definition, matrix.toString());
  }
}
