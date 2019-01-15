package stest.tron.wallet.node;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestNode001 {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
          .getStringList("fullnode.ip.list").get(1);
  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }
  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
            .usePlaintext(true)
            .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }


  @Test
  public void testGetAllNode() {
    GrpcAPI.NodeList nodeList = blockingStubFull
        .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
    Integer times = 0;
    while (nodeList.getNodesCount() == 0 && times++ < 60) {
      nodeList = blockingStubFull
              .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
      if (nodeList.getNodesCount() != 0) {
        break;
      }
      nodeList = blockingStubFull1
              .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
    }
    Assert.assertFalse(nodeList.getNodesCount() == 0);

    for (Integer j = 0; j < nodeList.getNodesCount(); j++) {
      Assert.assertTrue(nodeList.getNodes(j).hasAddress());
      Assert.assertFalse(nodeList.getNodes(j).getAddress().getHost().isEmpty());
      Assert.assertTrue(nodeList.getNodes(j).getAddress().getPort() < 65535);
      logger.info(ByteArray.toStr(nodeList.getNodes(j).getAddress().getHost().toByteArray()));
    }
    logger.info("get listnode succesuflly");

    //Improve coverage.
    GrpcAPI.NodeList newNodeList = blockingStubFull
        .listNodes(GrpcAPI.EmptyMessage.newBuilder().build());
    nodeList.equals(nodeList);
    nodeList.equals(newNodeList);
    nodeList.getNodesList();
    nodeList.hashCode();
    nodeList.isInitialized();

  }
  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


