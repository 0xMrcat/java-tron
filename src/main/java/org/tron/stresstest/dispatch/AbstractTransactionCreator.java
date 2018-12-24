package org.tron.stresstest.dispatch;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.stresstest.dispatch.strategy.Level2Strategy;

@Getter
public abstract class AbstractTransactionCreator extends Level2Strategy {
  protected String commonOwnerAddress = "TDZdB4ogHSgU1CGrun8WXaMb2QDDkvAKQm";
  protected String commonOwnerPrivateKey = "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";
  protected String commonToAddress = "TQjKWNDCLSgqUtg9vrjzZnWhhmsgNgTfmj";
  protected String commonToPrivateKey = "76fb5f55710c7ad6a98f73dd38a732f9a69a7b3ce700a694363a50572fa2842a";
  protected String commonWitnessAddress = "TXtrbmfwZ2LxtoCveEhZT86fTss1w8rwJE";
  protected String commonWitnessPrivateKey = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  protected String commonContractAddress1 = "TULWqmese4fpypNC1FyKGSDfHUJn1WkAdd";
  protected String commonContractAddress2 = "TEKtGxq16xmf6nDPaVYQ4wZXjrtBUjiuzi";
  protected String commonContractAddress3 = "TE2SuSLEgpnGjhWrA7DkAfi4BKQL2bX14k";

  long time = System.currentTimeMillis();
  AtomicLong count = new AtomicLong();
  public Transaction createTransaction(com.google.protobuf.Message message,
      ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());

    Transaction transaction = Transaction.newBuilder().setRawData(transactionBuilder.build())
        .build();

    long gTime = count.incrementAndGet() + time;
    String ref = "" + gTime;

    transaction = setReference(transaction, gTime, ByteArray.fromString(ref));

    transaction = setExpiration(transaction, gTime);

    return transaction;
  }

  private Transaction setReference(Transaction transaction, long blockNum,
      byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(blockHash))
        .setRefBlockBytes(ByteString.copyFrom(refBlockNum))
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  public Transaction setExpiration(Transaction transaction, long expiration) {
    Transaction.raw rawData = transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  public static Transaction sign(Transaction transaction, ECKey myKey) {
    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash = Sha256Hash.hash(transaction.getRawData().toByteArray());
    List<Contract> listContract = transaction.getRawData().getContractList();
    for (int i = 0; i < listContract.size(); i++) {
      ECDSASignature signature = myKey.sign(hash);
      ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
      transactionBuilderSigned.addSignature(
          bsSign);//Each contract may be signed with a different private key in the future.
    }

    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  public static org.tron.protos.Contract.ExchangeTransactionContract createExchangeTransactionContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant, long expected) {
    org.tron.protos.Contract.ExchangeTransactionContract.Builder builder = org.tron.protos.Contract.ExchangeTransactionContract
        .newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant)
        .setExpected(expected);
    return builder.build();
  }

  public static Sha256Hash getID(Transaction transaction) {
    return Sha256Hash.of(transaction.getRawData().toByteArray());
  }

  public static org.tron.protos.Contract.ExchangeInjectContract createExchangeInjectContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant) {
    org.tron.protos.Contract.ExchangeInjectContract.Builder builder = org.tron.protos.Contract.ExchangeInjectContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant);
    return builder.build();
  }

  public static org.tron.protos.Contract.ExchangeWithdrawContract createExchangeWithdrawContract(byte[] owner,
      long exchangeId, byte[] tokenId, long quant) {
    org.tron.protos.Contract.ExchangeWithdrawContract.Builder builder = org.tron.protos.Contract.ExchangeWithdrawContract
        .newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant);
    return builder.build();
  }

  public static org.tron.protos.Contract.ParticipateAssetIssueContract createParticipateAssetIssueContract(byte[] to,
      byte[] assertName, byte[] owner,
      long amount) {
    org.tron.protos.Contract.ParticipateAssetIssueContract.Builder builder = org.tron.protos.Contract.ParticipateAssetIssueContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    return builder.build();
  }

  public org.tron.protos.Contract.AssetIssueContract createAssetIssueContract(
      byte[] owner,
      String name,
      long totalSupply,
      int trxNum,
      int icoNum,
      int precision,
      long startTime,
      long endTime,
      int voteScore,
      String description,
      String url,
      long freeNetLimit,
      long publicFreeNetLimit,
      HashMap<String, String> frozenSupply
  ) {
    org.tron.protos.Contract.AssetIssueContract.Builder builder = org.tron.protos.Contract.AssetIssueContract
        .newBuilder();

    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setName(ByteString.copyFrom(name.getBytes()));

    builder.setTotalSupply(totalSupply);

    builder.setTrxNum(trxNum);

    builder.setNum(icoNum);

    builder.setPrecision(precision);

    long now = System.currentTimeMillis();

    builder.setStartTime(startTime);
    builder.setEndTime(endTime);
    builder.setVoteScore(voteScore);
    builder.setDescription(ByteString.copyFrom(description.getBytes()));
    builder.setUrl(ByteString.copyFrom(url.getBytes()));
    builder.setFreeAssetNetLimit(freeNetLimit);
    builder.setPublicFreeAssetNetLimit(publicFreeNetLimit);

    for (String daysStr : frozenSupply.keySet()) {
      String amountStr = frozenSupply.get(daysStr);
      long amount = Long.parseLong(amountStr);
      long days = Long.parseLong(daysStr);
      org.tron.protos.Contract.AssetIssueContract.FrozenSupply.Builder frozenSupplyBuilder
          = org.tron.protos.Contract.AssetIssueContract.FrozenSupply.newBuilder();
      frozenSupplyBuilder.setFrozenAmount(amount);
      frozenSupplyBuilder.setFrozenDays(days);
      builder.addFrozenSupply(frozenSupplyBuilder.build());
    }

    return builder.build();
  }

  public org.tron.protos.Contract.TriggerSmartContract triggerCallContract(byte[] address,
      byte[] contractAddress,
      long callValue, byte[] data) {
    org.tron.protos.Contract.TriggerSmartContract.Builder builder = org.tron.protos.Contract.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    return builder.build();
  }

  public FreezeBalanceContract createFreezeBalanceContract(byte[] ownerAddress, long frozen_balance,
      long frozen_duration, int resourceCode, String receiverAddress) {
    org.tron.protos.Contract.FreezeBalanceContract.Builder builder = org.tron.protos.Contract.FreezeBalanceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(ownerAddress);
    builder.setOwnerAddress(byteAddress).setFrozenBalance(frozen_balance)
        .setFrozenDuration(frozen_duration).setResourceValue(resourceCode);

    if(receiverAddress != null && !receiverAddress.equals("")){
      ByteString receiverAddressBytes = ByteString.copyFrom(
          Objects.requireNonNull(Wallet.decodeFromBase58Check(receiverAddress)));
      builder.setReceiverAddress(receiverAddressBytes);
    }
    return builder.build();
  }

  public UnfreezeBalanceContract createUnfreezeBalanceContract(byte[] ownerAddress, int resourceCode,String receiverAddress) {
    org.tron.protos.Contract.UnfreezeBalanceContract.Builder builder = org.tron.protos.Contract.UnfreezeBalanceContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(ownerAddress);
    builder.setOwnerAddress(byteAddreess).setResourceValue(resourceCode);

    if(receiverAddress != null && !receiverAddress.equals("")){
      ByteString receiverAddressBytes = ByteString.copyFrom(
          Objects.requireNonNull(Wallet.decodeFromBase58Check(receiverAddress)));
      builder.setReceiverAddress(receiverAddressBytes);
    }

    return builder.build();
  }

  public org.tron.protos.Contract.AccountCreateContract createAccountCreateContract(byte[] owner,
      byte[] address) {
    org.tron.protos.Contract.AccountCreateContract.Builder builder = org.tron.protos.Contract.AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(address));

    return builder.build();
  }

  public org.tron.protos.Contract.VoteWitnessContract createVoteWitnessContract(byte[] owner,
      HashMap<String, String> witness) {
    org.tron.protos.Contract.VoteWitnessContract.Builder builder = org.tron.protos.Contract.VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      long count = Long.parseLong(value);
      org.tron.protos.Contract.VoteWitnessContract.Vote.Builder voteBuilder = org.tron.protos.Contract.VoteWitnessContract.Vote
          .newBuilder();
      byte[] address = Wallet.decodeFromBase58Check(addressBase58);
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    return builder.build();
  }

  public org.tron.protos.Contract.WitnessUpdateContract createWitnessUpdateContract(byte[] owner,
      byte[] url) {
    org.tron.protos.Contract.WitnessUpdateContract.Builder builder = org.tron.protos.Contract.WitnessUpdateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUpdateUrl(ByteString.copyFrom(url));

    return builder.build();
  }

  public org.tron.protos.Contract.UpdateAssetContract createUpdateAssetContract(
      byte[] address,
      byte[] description,
      byte[] url,
      long newLimit,
      long newPublicLimit
  ) {
    org.tron.protos.Contract.UpdateAssetContract.Builder builder =
        org.tron.protos.Contract.UpdateAssetContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(address);
    builder.setDescription(ByteString.copyFrom(description));
    builder.setUrl(ByteString.copyFrom(url));
    builder.setNewLimit(newLimit);
    builder.setNewPublicLimit(newPublicLimit);
    builder.setOwnerAddress(basAddreess);

    return builder.build();
  }

  public org.tron.protos.Contract.UpdateSettingContract createUpdateSettingContract(byte[] owner,
      byte[] contractAddress, long consumeUserResourcePercent) {

    org.tron.protos.Contract.UpdateSettingContract.Builder builder = org.tron.protos.Contract.UpdateSettingContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    return builder.build();
  }

  public org.tron.protos.Contract.ExchangeCreateContract createExchangeCreateContract(byte[] owner,
      byte[] firstTokenId, long firstTokenBalance,
      byte[] secondTokenId, long secondTokenBalance) {
    org.tron.protos.Contract.ExchangeCreateContract.Builder builder = org.tron.protos.Contract.ExchangeCreateContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setFirstTokenId(ByteString.copyFrom(firstTokenId))
        .setFirstTokenBalance(firstTokenBalance)
        .setSecondTokenId(ByteString.copyFrom(secondTokenId))
        .setSecondTokenBalance(secondTokenBalance);
    return builder.build();
  }


  public org.tron.protos.Contract.ProposalCreateContract createProposalCreateContract(byte[] owner,
    HashMap<Long, Long> parametersMap) {
      org.tron.protos.Contract.ProposalCreateContract.Builder builder = org.tron.protos.Contract.ProposalCreateContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(owner));
      builder.putAllParameters(parametersMap);
      return builder.build();
    }

  public org.tron.protos.Contract.UpdateEnergyLimitContract createUpdateEnergyLimitContract(
      byte[] owner,
      byte[] contractAddress, long originEnergyLimit) {

    org.tron.protos.Contract.UpdateEnergyLimitContract.Builder builder = org.tron.protos.Contract.UpdateEnergyLimitContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setOriginEnergyLimit(originEnergyLimit);
    return builder.build();
  }
}
