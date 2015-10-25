package com.binarybirchtree.contributionart;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class BaseTest {
  protected static final String definition =
  ".....................................................\n" +
  "..!..!.!!!!.!...!....!!....!...!..!!..!!!!.!...!!!...\n" +
  "..!..!.!....!...!...!..!...!...!.!..!.!..!.!...!..!..\n" +
  "..!!!!.!!!..!...!...!..!...!...!.!..!.!!!..!...!..!..\n" +
  "..!..!.!....!...!...!..!...!.!.!.!..!.!..!.!...!..!..\n" +
  "..!..!.!!!!.!!!.!!!..!!.....!.!...!!..!..!.!!!.!!!...\n" +
  ".....................................................\n";

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  protected Path file;

  @Before
  public void create_definition_file () throws IOException {
    file = folder.newFile().toPath();
    try (BufferedWriter writer = Files.newBufferedWriter(file)) {
      writer.write(definition);
    }
  }
}
