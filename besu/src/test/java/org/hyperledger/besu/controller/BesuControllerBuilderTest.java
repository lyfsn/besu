/*
 * Copyright 2019 ConsenSys AG.
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
package org.hyperledger.besu.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mockStatic;

import org.hyperledger.besu.config.CheckpointConfigOptions;
import org.hyperledger.besu.config.EthashConfigOptions;
import org.hyperledger.besu.config.GenesisConfigFile;
import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.cryptoservices.NodeKey;
import org.hyperledger.besu.cryptoservices.NodeKeyUtils;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.GasLimitCalculator;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.chain.GenesisState;
import org.hyperledger.besu.ethereum.chain.VariablesStorage;
import org.hyperledger.besu.ethereum.core.MiningParameters;
import org.hyperledger.besu.ethereum.core.PrivacyParameters;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockDataGenerator;
import org.hyperledger.besu.ethereum.eth.EthProtocolConfiguration;
import org.hyperledger.besu.ethereum.eth.sync.SynchronizerConfiguration;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.p2p.config.NetworkingConfiguration;
import org.hyperledger.besu.ethereum.privacy.storage.PrivateStateStorage;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.ethereum.storage.keyvalue.VariablesKeyValueStorage;
import org.hyperledger.besu.ethereum.trie.bonsai.cache.CachedMerkleTrieLoader;
import org.hyperledger.besu.ethereum.trie.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.trie.bonsai.worldview.BonsaiWorldState;
import org.hyperledger.besu.ethereum.trie.forest.pruner.PrunerConfiguration;
import org.hyperledger.besu.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration;
import org.hyperledger.besu.ethereum.worldstate.ImmutableDataStorageConfiguration;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.ethereum.worldstate.WorldStatePreimageStorage;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorageCoordinator;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.metrics.ObservableMetricsSystem;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.DataStorageFormat;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;

import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.OptionalLong;

import com.google.common.collect.Range;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BesuControllerBuilderTest {

  private BesuControllerBuilder besuControllerBuilder;
  private static final NodeKey nodeKey = NodeKeyUtils.generate();

  @Mock GenesisConfigFile genesisConfigFile;
  @Mock GenesisConfigOptions genesisConfigOptions;
  @Mock EthashConfigOptions ethashConfigOptions;
  @Mock CheckpointConfigOptions checkpointConfigOptions;
  @Mock SynchronizerConfiguration synchronizerConfiguration;
  @Mock EthProtocolConfiguration ethProtocolConfiguration;
  @Mock PrivacyParameters privacyParameters;
  @Mock Clock clock;
  @Mock StorageProvider storageProvider;
  @Mock GasLimitCalculator gasLimitCalculator;
  @Mock WorldStateArchive worldStateArchive;
  @Mock BonsaiWorldStateKeyValueStorage bonsaiWorldStateStorage;
  @Mock WorldStatePreimageStorage worldStatePreimageStorage;
  private final TransactionPoolConfiguration poolConfiguration =
      TransactionPoolConfiguration.DEFAULT;
  private final MiningParameters miningParameters = MiningParameters.newDefault();

  private final ObservableMetricsSystem observableMetricsSystem = new NoOpMetricsSystem();

  BigInteger networkId = BigInteger.ONE;

  @TempDir Path tempDir;

  @BeforeEach
  public void setup() {

    final ForestWorldStateKeyValueStorage worldStateKeyValueStorage =
        mock(ForestWorldStateKeyValueStorage.class);
    final WorldStateStorageCoordinator worldStateStorageCoordinator =
        new WorldStateStorageCoordinator(worldStateKeyValueStorage);

    when(genesisConfigFile.getParentHash()).thenReturn(Hash.ZERO.toHexString());
    when(genesisConfigFile.getDifficulty()).thenReturn(Bytes.of(0).toHexString());
    when(genesisConfigFile.getExtraData()).thenReturn(Bytes.EMPTY.toHexString());
    when(genesisConfigFile.getMixHash()).thenReturn(Hash.ZERO.toHexString());
    when(genesisConfigFile.getNonce()).thenReturn(Long.toHexString(1));
    when(genesisConfigFile.getConfigOptions(any())).thenReturn(genesisConfigOptions);
    when(genesisConfigFile.getConfigOptions()).thenReturn(genesisConfigOptions);
    when(genesisConfigOptions.getThanosBlockNumber()).thenReturn(OptionalLong.empty());
    when(genesisConfigOptions.getEthashConfigOptions()).thenReturn(ethashConfigOptions);
    when(genesisConfigOptions.getCheckpointOptions()).thenReturn(checkpointConfigOptions);
    when(ethashConfigOptions.getFixedDifficulty()).thenReturn(OptionalLong.empty());
    when(storageProvider.getStorageBySegmentIdentifier(any()))
        .thenReturn(new InMemoryKeyValueStorage());
    when(storageProvider.createBlockchainStorage(any(), any()))
        .thenReturn(
            new KeyValueStoragePrefixedKeyBlockchainStorage(
                new InMemoryKeyValueStorage(),
                new VariablesKeyValueStorage(new InMemoryKeyValueStorage()),
                new MainnetBlockHeaderFunctions()));
    when(synchronizerConfiguration.getDownloaderParallelism()).thenReturn(1);
    when(synchronizerConfiguration.getTransactionsParallelism()).thenReturn(1);
    when(synchronizerConfiguration.getComputationParallelism()).thenReturn(1);

    when(synchronizerConfiguration.getBlockPropagationRange()).thenReturn(Range.closed(1L, 2L));

    lenient()
        .when(
            storageProvider.createWorldStateStorageCoordinator(
                DataStorageConfiguration.DEFAULT_FOREST_CONFIG))
        .thenReturn(worldStateStorageCoordinator);
    lenient()
        .when(storageProvider.createWorldStatePreimageStorage())
        .thenReturn(worldStatePreimageStorage);

    lenient().when(worldStateKeyValueStorage.isWorldStateAvailable(any())).thenReturn(true);
    lenient()
        .when(worldStatePreimageStorage.updater())
        .thenReturn(mock(WorldStatePreimageStorage.Updater.class));
    lenient()
        .when(worldStateKeyValueStorage.updater())
        .thenReturn(mock(ForestWorldStateKeyValueStorage.Updater.class));

    besuControllerBuilder = spy(visitWithMockConfigs(new MainnetBesuControllerBuilder()));
  }

  BesuControllerBuilder visitWithMockConfigs(final BesuControllerBuilder builder) {
    return builder
        .gasLimitCalculator(gasLimitCalculator)
        .genesisConfigFile(genesisConfigFile)
        .synchronizerConfiguration(synchronizerConfiguration)
        .ethProtocolConfiguration(ethProtocolConfiguration)
        .miningParameters(miningParameters)
        .metricsSystem(observableMetricsSystem)
        .privacyParameters(privacyParameters)
        .dataDirectory(tempDir)
        .clock(clock)
        .transactionPoolConfiguration(poolConfiguration)
        .nodeKey(nodeKey)
        .storageProvider(storageProvider)
        .evmConfiguration(EvmConfiguration.DEFAULT)
        .networkConfiguration(NetworkingConfiguration.create())
        .networkId(networkId);
  }

  @Test
  public void shouldDisablePruningIfBonsaiIsEnabled() {
    DataStorageConfiguration dataStorageConfiguration =
        ImmutableDataStorageConfiguration.builder()
            .dataStorageFormat(DataStorageFormat.BONSAI)
            .bonsaiMaxLayersToLoad(DataStorageConfiguration.DEFAULT_BONSAI_MAX_LAYERS_TO_LOAD)
            .build();
    BonsaiWorldState mockWorldState = mock(BonsaiWorldState.class, Answers.RETURNS_DEEP_STUBS);
    doReturn(worldStateArchive)
        .when(besuControllerBuilder)
        .createWorldStateArchive(
            any(WorldStateStorageCoordinator.class),
            any(Blockchain.class),
            any(CachedMerkleTrieLoader.class));
    doReturn(mockWorldState).when(worldStateArchive).getMutable();
    when(storageProvider.createWorldStateStorageCoordinator(dataStorageConfiguration))
        .thenReturn(new WorldStateStorageCoordinator(bonsaiWorldStateStorage));
    besuControllerBuilder.isPruningEnabled(true).dataStorageConfiguration(dataStorageConfiguration);

    besuControllerBuilder.build();

    verify(storageProvider, never())
        .getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.PRUNING_STATE);
  }

  @Test
  public void shouldUsePruningIfForestIsEnabled() {
    besuControllerBuilder
        .isPruningEnabled(true)
        .pruningConfiguration(new PrunerConfiguration(1, 2))
        .dataStorageConfiguration(
            ImmutableDataStorageConfiguration.builder()
                .dataStorageFormat(DataStorageFormat.FOREST)
                .bonsaiMaxLayersToLoad(DataStorageConfiguration.DEFAULT_BONSAI_MAX_LAYERS_TO_LOAD)
                .build());
    besuControllerBuilder.build();

    verify(storageProvider).getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.PRUNING_STATE);
  }

  @Test
  public void shouldNotUseCachedGenesisStateHashFirstBuild() {
    DataStorageConfiguration dataStorageConfiguration =
            ImmutableDataStorageConfiguration.builder()
                    .dataStorageFormat(DataStorageFormat.BONSAI)
                    .bonsaiMaxLayersToLoad(DataStorageConfiguration.DEFAULT_BONSAI_MAX_LAYERS_TO_LOAD)
                    .build();
    BonsaiWorldState mockWorldState = mock(BonsaiWorldState.class, Answers.RETURNS_DEEP_STUBS);
    doReturn(worldStateArchive)
            .when(besuControllerBuilder)
            .createWorldStateArchive(
                    any(WorldStateStorageCoordinator.class),
                    any(Blockchain.class),
                    any(CachedMerkleTrieLoader.class));
    doReturn(mockWorldState).when(worldStateArchive).getMutable();
    when(storageProvider.createWorldStateStorageCoordinator(dataStorageConfiguration))
            .thenReturn(new WorldStateStorageCoordinator(bonsaiWorldStateStorage));
    besuControllerBuilder.dataStorageConfiguration(dataStorageConfiguration);
    besuControllerBuilder.useCachedGenesisStateHash(true);

    VariablesStorage mockStorage = mock(VariablesStorage.class);
    when(storageProvider.createVariablesStorage()).thenReturn(mockStorage);
    VariablesStorage.Updater mockUpdater = mock(VariablesStorage.Updater.class);
    when(mockStorage.updater()).thenReturn(mockUpdater);

    besuControllerBuilder.build();

    verify(mockStorage, times(0)).getGenesisStateHash();
    verify(mockUpdater, times(1)).setGenesisStateHash(any());
    verify(mockUpdater, times(1)).commit();
  }

  @Test
  public void shouldUseCachedGenesisStateHashSecondBuild() {
    DataStorageConfiguration dataStorageConfiguration =
            ImmutableDataStorageConfiguration.builder()
                    .dataStorageFormat(DataStorageFormat.BONSAI)
                    .bonsaiMaxLayersToLoad(DataStorageConfiguration.DEFAULT_BONSAI_MAX_LAYERS_TO_LOAD)
                    .build();
    BonsaiWorldState mockWorldState = mock(BonsaiWorldState.class, Answers.RETURNS_DEEP_STUBS);
    doReturn(worldStateArchive)
            .when(besuControllerBuilder)
            .createWorldStateArchive(
                    any(WorldStateStorageCoordinator.class),
                    any(Blockchain.class),
                    any(CachedMerkleTrieLoader.class));
    doReturn(mockWorldState).when(worldStateArchive).getMutable();
    when(storageProvider.createWorldStateStorageCoordinator(dataStorageConfiguration))
            .thenReturn(new WorldStateStorageCoordinator(bonsaiWorldStateStorage));
    besuControllerBuilder.dataStorageConfiguration(dataStorageConfiguration);
    besuControllerBuilder.useCachedGenesisStateHash(true);

    VariablesStorage mockStorage = mock(VariablesStorage.class);
    when(storageProvider.createVariablesStorage()).thenReturn(mockStorage);
    when(mockStorage.getGenesisStateHash()).thenReturn(Optional.of(Hash.fromHexString("0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421")));
    VariablesStorage.Updater mockUpdater = mock(VariablesStorage.Updater.class);
    lenient().when(mockStorage.updater()).thenReturn(mockUpdater);

    besuControllerBuilder.build();

    verify(mockStorage, times(1)).getGenesisStateHash();
    verify(mockUpdater, times(0)).setGenesisStateHash(any());
    verify(mockUpdater, times(0)).commit();

    try (MockedStatic<GenesisState> mockedStatic = mockStatic(GenesisState.class)) {

//      GenesisState mockedGenesisState = mock(GenesisState.class);

//      final BlockDataGenerator gen = new BlockDataGenerator(1);
//      final BlockDataGenerator.BlockOptions blockOptions = BlockDataGenerator.BlockOptions.create();
//      blockOptions.setBlockNumber(0L);
//      blockOptions.setStateRoot(Hash.fromHexString("0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"));
//      final Block block = gen.block(blockOptions);


      ProtocolSchedule protocolSchedule = mock(ProtocolSchedule.class);
      GenesisState genesisState = GenesisState.fromConfig(dataStorageConfiguration, genesisConfigFile, protocolSchedule);

//      when(mockedGenesisState.getBlock()).thenReturn(block);

      mockedStatic.when(() -> GenesisState.fromConfig(any(DataStorageConfiguration.class), any(GenesisConfigFile.class), any(ProtocolSchedule.class)))
              .thenReturn(genesisState);
      mockedStatic.when(() -> GenesisState.fromConfig(any(Hash.class), any(GenesisConfigFile.class), any(ProtocolSchedule.class)))
              .thenReturn(genesisState);

      besuControllerBuilder.build();

      mockedStatic.verify(() -> GenesisState.fromConfig(any(DataStorageConfiguration.class), any(GenesisConfigFile.class), any(ProtocolSchedule.class)), times(0));
      mockedStatic.verify(() -> GenesisState.fromConfig(any(Hash.class), any(GenesisConfigFile.class), any(ProtocolSchedule.class)), times(1));
    }
  }
}
