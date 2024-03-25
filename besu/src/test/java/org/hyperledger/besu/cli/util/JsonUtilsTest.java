/*
 * Copyright Hyperledger Besu Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.cli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonUtilsTest {

  @Test
  void shouldReadJsonFromFileExcludingAllocField() {
    String genesisStingWithoutAllocField =
        "{\"config\":{\"chainId\":1, \"homesteadBlock\":1150000, \"daoForkBlock\":1920000, \"eip150Block\":2463000, \"eip158Block\":2675000, \"byzantiumBlock\":4370000, \"petersburgBlock\":7280000, \"istanbulBlock\":9069000, \"muirGlacierBlock\":9200000, \"berlinBlock\":12244000, \"londonBlock\":12965000, \"arrowGlacierBlock\":13773000, \"grayGlacierBlock\":15050000, \"terminalTotalDifficulty\":58750000000000000000000, \"shanghaiTime\":1681338455, \"cancunTime\":1710338135, \"ethash\":{}, \"discovery\":{\"dns\":\"enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.mainnet.ethdisco.net\", \"bootnodes\":[\"enode://d860a01f9722d78051619d1e2351aba3f43f943f6f00718d1b9baa4101932a1f5011f16bb2b1bb35db20d6fe28fa0bf09636d26a87d31de9ec6203eeedb1f666@18.138.108.67:30303\", \"enode://22a8232c3abc76a16ae9d6c3b164f98775fe226f0917b0ca871128a74a8e9630b458460865bab457221f1d448dd9791d24c4e5d88786180ac185df813a68d4de@3.209.45.79:30303\", \"enode://2b252ab6a1d0f971d9722cb839a42cb81db019ba44c08754628ab4a823487071b5695317c8ccd085219c3a03af063495b2f1da8d18218da2d6a82981b45e6ffc@65.108.70.101:30303\", \"enode://4aeb4ab6c14b23e2c4cfdce879c04b0748a20d8e9b59e25ded2a08143e265c6c25936e74cbc8e641e3312ca288673d91f2f93f8e277de3cfa444ecdaaf982052@157.90.35.166:30303\"]}, \"checkpoint\":{\"hash\":\"0x44bca881b07a6a09f83b130798072441705d9a665c5ac8bdf2f39a3cdf3bee29\", \"number\":11052984, \"totalDifficulty\":\"0x3D103014E5C74E5E196\"}}, \"nonce\":\"0x42\", \"timestamp\":\"0x0\", \"extraData\":\"0x11bbe8db4e347b4e8c937c1c8370e4b5ed33adb3db69cbdb7a38e1e50b1b82fa\", \"gasLimit\":\"0x1388\", \"difficulty\":\"0x400000000\", \"mixHash\":\"0x0000000000000000000000000000000000000000000000000000000000000000\", \"coinbase\":\"0x0000000000000000000000000000000000000000\"}";
    File genesisFile = new File("src/test/resources/org/hyperledger/besu/cli/util/besu.json");

    String genesisStringFromFile = JsonUtils.readJsonExcludingField(genesisFile, "alloc");
    assertEquals(genesisStingWithoutAllocField, genesisStringFromFile);
  }
}
