package stest.tron.wallet.mutisign.accountPermissionUpdate;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class accountPermissionUpdate018 {

    private final String testKey002 = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key1");
    private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

    private final String witnessKey001 = Configuration.getByPath("testng.conf")
        .getString("witness.key1");
    private final byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);

    private final String contractTRONdiceAddr = "TMYcx6eoRXnePKT1jVn25ZNeMNJ6828HWk";

    private ECKey ecKey1 = new ECKey(Utils.getRandom());
    private byte[] ownerAddress = ecKey1.getAddress();
    private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    private ECKey ecKey2 = new ECKey(Utils.getRandom());
    private byte[] normalAddr001 = ecKey2.getAddress();
    private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    private ECKey tmpECKey01 = new ECKey(Utils.getRandom());
    private byte[] tmpAddr01 = tmpECKey01.getAddress();
    private String tmpKey01 = ByteArray.toHexString(tmpECKey01.getPrivKeyBytes());

    private ECKey tmpECKey02 = new ECKey(Utils.getRandom());
    private byte[] tmpAddr02 = tmpECKey02.getAddress();
    private String tmpKey02 = ByteArray.toHexString(tmpECKey02.getPrivKeyBytes());

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private String fullnode = Configuration.getByPath("testng.conf")
        .getStringList("fullnode.ip.list").get(0);
    private long maxFeeLimit = Configuration.getByPath("testng.conf")
        .getLong("defaultParameter.maxFeeLimit");

    private static final long now = System.currentTimeMillis();
    private static String tokenName = "testAssetIssue_" + Long.toString(now);
    private static ByteString assetAccountId = null;
    private static final long TotalSupply = 1000L;
    private byte[] transferTokenContractAddress = null;

    private String description = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetDescription");
    private String url = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetUrl");


    @BeforeSuite
    public void beforeSuite() {
      Wallet wallet = new Wallet();
      Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    }

    @BeforeClass(enabled = true)
    public void beforeClass() {

      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
      PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    }

    @Test
  public void testWitnessType01() {
    // type = 1
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

      Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
          ownerAddress, ownerKey, blockingStubFull,
          ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

      ownerPermissionKeys.clear();
      ownerPermissionKeys.add(testKey002);

      Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
          PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

      Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getOwnerPermission().getKeysCount());

      Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getWitnessPermission().getKeysCount());

      PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getActivePermissionList());

      System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getOwnerPermission()));

      System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getWitnessPermission()));

      PublicMethedForMutiSign.recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
  }

  @Test
  public void testWitnessType02() {
    // type = 2
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":2,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessType03() {
    // type = 0
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":0,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessType04() {
    // type = -1
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":-1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }


  @Test
  public void testWitnessType05() {
    // type = long.min
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":-9223372036854775808,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessType06() {
    // type = long.min - 1000020
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":-9223372036855775828,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());

  }

  @Test
  public void testWitnessType07() {
    // type = "12a"
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

   String accountPermissionJson =
      "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
          + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
          + "\",\"weight\":1}]},"
          + "\"witness_permission\":{\"type\":\"12a\",\"threshold\":1,\"keys\":["
          + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
          + "\",\"weight\":1}]},"
          + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
          + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
          + "\"keys\":["
          + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
          + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
          + "]}]}";

    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e){
      logger.info("NumberFormatException: For input string: \"12a\"");
      ret = true;
    }
    Assert.assertTrue(ret);
  }

  @Test
  public void testWitnessType08() {
    // type = ""
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
        + "\",\"weight\":1}]},"
        + "\"witness_permission\":{\"type\":\"\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
        + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";
    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e){
      logger.info("NullPointerException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }

  @Test
  public void testWitnessType09() {
    // type =
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (com.alibaba.fastjson.JSONException e){
      logger.info("JSONException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }

  @Test
  public void testWitnessType10() {
    // type = null
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":"+ null +",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e){
      logger.info("NullPointerException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }

  @Test
  public void testWitnessType11() {
    // type = Integer.MAX_VALUE
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":2147483647,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessType12() {
    // type = Long.MAX_VALUE
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":9223372036854775807,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessType13() {
    // type = long.MAX_VALUE + 1
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":9223372036854775808,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessType14() {
    // type = long.min - 1
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":-9223372036854775809,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessType15() {
    // type = Long.MAX_VALUE + 1
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":9223372036854775808,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
    accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : witness permission type is error",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessType16() {
    // type = 1.5
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002)
            + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1.5,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission()));

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getWitnessPermission()));

    PublicMethedForMutiSign.recoverWitnessPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
