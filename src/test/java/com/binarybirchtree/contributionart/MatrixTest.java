// This file is part of ContributionArt.
// Copyright (C) 2015, 2016 Binary Birch Tree
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
