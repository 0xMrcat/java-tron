package stest.tron.wallet.multiSign.permissionFunction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class MultiSignAddKey007 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private final String testWitnesses = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] WitnessesKey = PublicMethed.getFinalAddress(testWitnesses);
  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidityInFullnode = null;

  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSolidityInFullnode = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);


  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

  }


  @Test
  public void testMultiSignAddKey() {
    //循环，删除一个，添加一个，修改一个
//    while(true) {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] testAddress = ecKey.getAddress();
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey1.getAddress();
    String sendAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] test002Address = ecKey2.getAddress();
    String sendAccountKey2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] test003Address = ecKey3.getAddress();
    String sendAccountKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    ECKey ecKey4 = new ECKey(Utils.getRandom());
    byte[] test004Address = ecKey4.getAddress();
    String sendAccountKey4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
    ECKey ecKey5 = new ECKey(Utils.getRandom());
    byte[] test005Address = ecKey5.getAddress();
    String sendAccountKey5 = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 1000000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(test002Address, 1000000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(testAddress, 1000000000L, fromAddress, testKey002,
            blockingStubFull));

    String accountPermissionJson =
        "[{\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey4)
            + "\",\"weight\":1}]},"
            + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":10,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey)
            + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
            + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
            + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey4)
            + "\",\"weight\":2}]}]";
    PublicMethed
        .accountPermissionUpdate(accountPermissionJson, testAddress, dev001Key, blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(testAddress, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getPermissionsList();
    printPermissionList(permissionsList);
    String[] permissionKeyString = new String[5];
    permissionKeyString[0] = dev001Key;
    permissionKeyString[1] = sendAccountKey2;
    permissionKeyString[2] = sendAccountKey;
    permissionKeyString[3] = sendAccountKey4;
    permissionKeyString[4] = sendAccountKey3;
    String permission = "owner";
    Assert.assertTrue(PublicMethed
        .permissionDeleteKey(permission, test002Address, testAddress, dev001Key,
            blockingStubFull));
    Account test001AddressAccount2 = PublicMethed.queryAccount(testAddress, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getPermissionsList();
    printPermissionList(permissionsList2);
    Assert.assertTrue(PublicMethed
        .permissionAddKey(permission, test002Address, 2, testAddress, dev001Key,
            blockingStubFull));

    Assert.assertTrue(PublicMethed
        .permissionUpdateKey(permission, test002Address, 3, testAddress, dev001Key,
            blockingStubFull));
    Account test001AddressAccount1 = PublicMethed.queryAccount(testAddress, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getPermissionsList();
    printPermissionList(permissionsList1);

    //}
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public static void getPermissionsList(byte[] dev001Address,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Account contractAccount = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    List<Permission> permissionsList = contractAccount.getPermissionsList();
    for (int i = 0; i < permissionsList.size(); i++) {
      System.out.println(permissionsList.get(i));
    }
//    printPermissionList(permissionsList);
  }


  public static void printPermissionList(List<Permission> permissionList) {
    String result = "\n";
    result += "[";
    result += "\n";
    int i = 0;
    for (Permission permission : permissionList) {
      result += "permission " + i + " :::";
      result += "\n";
      result += "{";
      result += "\n";
      result += printPermission(permission);
      result += "\n";
      result += "}";
      result += "\n";
      i++;
    }
    result += "]";
    System.out.println(result);
  }

  public static String printPermission(Permission permission) {
    StringBuffer result = new StringBuffer();
    result.append("name: ");
    result.append(permission.getName());
    result.append("\n");
    result.append("threshold: ");
    result.append(permission.getThreshold());
    result.append("\n");
    if (permission.getKeysCount() > 0) {
      result.append("keys:");
      result.append("\n");
      result.append("[");
      result.append("\n");
      for (Key key : permission.getKeysList()) {
        result.append(printKey(key));
      }
      result.append("]");
      result.append("\n");
    }
    return result.toString();
  }

  public static String printKey(Key key) {
    StringBuffer result = new StringBuffer();
    result.append("address: ");
    result.append(encode58Check(key.getAddress().toByteArray()));
    result.append("\n");
    result.append("weight: ");
    result.append(key.getWeight());
    result.append("\n");
    return result.toString();
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }
}
