package org.hyperledger.besu.cli.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

class JsonUtilsTest {

  @Test
  void readJsonExcludingField() {
    File genesisFile = new File("genesis.json");
    String s = JsonUtils.readJsonExcludingField(genesisFile, "");
    assertEquals("", s);
  }
}
