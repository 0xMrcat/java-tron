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
public class accountPermissionUpdate016 {

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
  public void testWitnessWeight01() {
    // weight = Integer.MIN_VALUE
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":-2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

      GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

      Assert.assertFalse(response.getResult());
      Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
      Assert.assertEquals("contract validate error : key's weight"
              + " should be greater than 0",
          response.getMessage().toStringUtf8());
  }

  @Test
  public void testWitnessWeight02() {
    // weight = 0
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":0}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : key's weight"
            + " should be greater than 0",
        response.getMessage().toStringUtf8());
  }


  @Test
  public void testWitnessWeight03() {
    // weight = -1
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":-1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : key's weight"
            + " should be greater than 0",
        response.getMessage().toStringUtf8());
  }


  @Test
  public void testWitnessWeight04() {
    // weight = long.min
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":-9223372036854775808}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : key's weight"
            + " should be greater than 0",
        response.getMessage().toStringUtf8());
  }


  @Test
  public void testWitnessWeight05() {
    // weight = long.min - 1000020
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);
    PublicMethed.printAddress(witnessKey001);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":-9223372036855775828}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
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
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }


  @Test
  public void testWitnessWeight06() {
    // weight = "12a"
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":\"12a\"}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
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
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }


  @Test
  public void testWitnessWeight07() {
    // weight = ""
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":\"\"}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
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
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }


  @Test
  public void testWitnessWeight08() {
    // weight =
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
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
  public void testWitnessWeight09() {
    // weight = null
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":"+ null +"}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
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
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }

  @Test
  public void testWitnessWeight10() {
    // weight = 1
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
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
  public void testWitnessWeight11() {
    // weight = Integer.MAX_VALUE
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":2147483647}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
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
  public void testWitnessWeight12() {
    //  Long.MAX_VALUE + 1
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
      "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
          + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
          + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
          + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
          + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
          + "\",\"weight\":9223372036854775808}]},"
          + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
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
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }

  @Test
  public void testWitnessWeight13() {
    // weight = Long.MAX_VALUE
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":9223372036854775807,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":9223372036854775807}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
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
  public void testWitnessWeight14() {
    // weight = 1.1
    String ownerKey  = witnessKey001;
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
            + "\"witness_permission\":{\"type\":1,\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1.1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
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
      logger.info("NumberFormatException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
