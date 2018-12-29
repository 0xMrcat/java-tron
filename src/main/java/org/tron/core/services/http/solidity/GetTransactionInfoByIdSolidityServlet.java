package org.tron.core.services.http.solidity;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.WalletSolidity;
import org.tron.core.services.http.JsonFormat;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfoV2;


@Component
@Slf4j
public class GetTransactionInfoByIdSolidityServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getParameter("value");
      TransactionInfoV2 transInfoV2 = wallet.getTransactionInfoV2ById(ByteString.copyFrom(
          ByteArray.fromHexString(input)));
      if (transInfoV2 != null) {
        response.getWriter().println(JsonFormat.printToString(transInfoV2));
      } else {
        TransactionInfo transInfo = wallet.getTransactionInfoById(ByteString.copyFrom(
            ByteArray.fromHexString(input)));
        if (transInfo != null) {
          response.getWriter().println(JsonFormat.printToString(transInfo));
        } else {
          response.getWriter().println("{}");
        }
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(e.getMessage());
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build);
      TransactionInfoV2 transInfoV2 = wallet.getTransactionInfoV2ById(ByteString.copyFrom(
          ByteArray.fromHexString(input)));
      if (transInfoV2 != null) {
        response.getWriter().println(JsonFormat.printToString(transInfoV2));
      } else {
        TransactionInfo transInfo = wallet.getTransactionInfoById(build.build().getValue());
        if (transInfo != null) {
          response.getWriter().println(JsonFormat.printToString(transInfo));
        } else {
          response.getWriter().println("{}");
        }
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(e.getMessage());
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}
