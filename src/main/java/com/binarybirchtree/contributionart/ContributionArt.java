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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

class ContributionArt {
  private static final Logger LOGGER = Logger.getLogger(ContributionArt.class.getName());

  public static void main (String[] args) {
    class Arguments {
      @Parameter(names = { "-m", "--matrix" }, description = "Matrix definition path.", required = true)
      private String matrix;

      @Parameter(names = { "-r", "--repo" }, description = "Repository path.", required = true)
      private String repository;

      @Parameter(names = { "-f", "--factor" }, description = "Scaling factor.")
      private int factor = 10;

      @Parameter(names = { "-n", "--name" }, description = "User name.")
      private String name = "";

      @Parameter(names = { "-e", "--email" }, description = "Email address.")
      private String email = "";
    }

    try {
      Arguments arguments = new Arguments();
      new JCommander(arguments, args);

      try (Repository repository = new Repository(Paths.get(arguments.repository), arguments.name, arguments.email)) {
        repository.illustrate(new Matrix(Paths.get(arguments.matrix)), arguments.factor);
      }
    }
    catch (ParameterException error) {
      System.err.println(error.getMessage());
    }
    catch (IOException | Matrix.FileFormatException | Repository.GitException error) {
      LOGGER.severe(error.toString());
    }
  }
}