package org.tron.core.db;

import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.StoreException;

@Slf4j
@Component
public class TransactionStore extends TronStoreWithRevoking<TransactionCapsule> {

  @Autowired
  private BlockStore blockStore;

  @Autowired
  private TransactionStore(@Value("trans") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, TransactionCapsule item) {
    if (Objects.isNull(item) || item.getBlockNum() == -1) {
      super.put(key, item);
    } else {
      revokingDB.put(key, ByteArray.fromLong(item.getBlockNum()));
    }

    if (Objects.nonNull(indexHelper)) {
      indexHelper.update(item.getInstance());
    }
  }

  @Override
  public TransactionCapsule get(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    if (value.length == 8) {
      List<BlockCapsule> blocksList = blockStore.getLimitNumber(ByteArray.toLong(value), 1);
      if (blocksList.size() != 0) {
        for (TransactionCapsule e : blocksList.get(0).getTransactions()) {
          if (e.getTransactionId().equals(Sha256Hash.wrap(key))) {
            return e;
          }
        }
      }
    }

    return new TransactionCapsule(value);
  }

  /**
   * get total transaction.
   */
  @Deprecated
  public long getTotalTransactions() {
    return 0; //Streams.stream(iterator()).count();
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      TransactionCapsule item;
      try {
        item = get(key);
        if (Objects.nonNull(item)) {
          indexHelper.remove(item.getInstance());
        }
      } catch (StoreException e) {
        return;
      }
    }
  }
}
