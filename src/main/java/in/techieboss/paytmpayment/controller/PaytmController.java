package in.techieboss.paytmpayment.controller;

import com.paytm.merchant.models.PaymentDetail;
import com.paytm.merchant.models.PaymentStatusDetail;
import com.paytm.merchant.models.SDKResponse;
import com.paytm.merchant.models.Time;
import com.paytm.pg.Payment;
import com.paytm.pg.constants.LibraryConstants;
import com.paytm.pg.constants.MerchantProperties;
import com.paytm.pgplus.enums.EChannelId;
import com.paytm.pgplus.enums.EnumCurrency;
import com.paytm.pgplus.models.Money;
import com.paytm.pgplus.models.UserInfo;
import com.paytm.pgplus.response.InitiateTransactionResponse;
import com.paytm.pgplus.response.NativePaymentStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Controller
@RequestMapping("/payments")
public class PaytmController {

    @Value("${paytm.payment.merchantId}")
    private String merchantId;
    @Value("${paytm.payment.merchantKey}")
    private String merchantKey;
    @Value("${paytm.payment.callbackUrl}")
    private String callBackUrl;

    @GetMapping(value = "/welcome")
    public ModelAndView welcome() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("home");
        return modelAndView;
    }

    @PostMapping("/initiate")
    public ModelAndView initiatePayment(@RequestParam("amount") String amount) throws Exception {
        String environment = LibraryConstants.PRODUCTION_ENVIRONMENT;
        String website = "DEFAULT";
        String client_id = getRandomNumber();

        MerchantProperties.setCallbackUrl(callBackUrl);

        MerchantProperties.initialize(environment, merchantId, merchantKey, client_id, website);
        LibraryConstants.LOGGER.setLevel(Level.ALL);

        EChannelId channelId = EChannelId.WEB;
        String orderId = getRandomNumber() + "_" + System.currentTimeMillis();
        Money txnAmount = new Money(EnumCurrency.INR, amount);
        UserInfo userInfo = new UserInfo();
        userInfo.setCustId(getRandomNumber());
        PaymentDetail paymentDetails = new PaymentDetail.PaymentDetailBuilder(channelId, orderId, txnAmount, userInfo).build();
        SDKResponse<InitiateTransactionResponse> response = Payment.createTxnToken(paymentDetails);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("initiate-transaction");
        modelAndView.addObject("txnToken", response.getResponseObject().getBody().getTxnToken());
        modelAndView.addObject("orderId", orderId);
        modelAndView.addObject("amount", amount);
        modelAndView.addObject("mid", merchantId);
        return modelAndView;
    }

    @PostMapping(value = "/response", consumes = {"application/x-www-form-urlencoded"})
    public ModelAndView getResponse(@RequestParam String ORDERID) {
        String message = null;
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("initiate-transaction");
        try {
            // Retrieve the request body
            System.out.println("inside callback: " + ORDERID);
            Time readTimeout = new Time(5, TimeUnit.MINUTES);
            PaymentStatusDetail paymentStatusDetail = new PaymentStatusDetail.PaymentStatusDetailBuilder(ORDERID).setReadTimeout(readTimeout).build();
            SDKResponse<NativePaymentStatusResponse> response = Payment.getPaymentStatus(paymentStatusDetail);
            System.out.println("handle callback succesfully");
            message = response.getResponseObject().getBody().getResultInfo().getResultStatus();

            modelAndView.setViewName("output");
            modelAndView.addObject("message", message);
            modelAndView.addObject("txnId", response.getResponseObject().getBody().getTxnId());
            modelAndView.addObject("orderId", response.getResponseObject().getBody().getOrderId());
            modelAndView.addObject("txnAmount", response.getResponseObject().getBody().getTxnAmount());
            modelAndView.addObject("txnDate", response.getResponseObject().getBody().getTxnDate());
            modelAndView.addObject("gatewayName", response.getResponseObject().getBody().getGatewayName());
        } catch (Exception e) {
            System.out.println(e);
            return modelAndView;
        }
        return modelAndView;
    }

    private String getRandomNumber() {
        Random random = new Random();
        long randomNumber = random.nextLong() % 1000000000L;

        if (randomNumber < 0) {
            randomNumber *= -1;
        }

        String formattedNumber = String.format("%09d", randomNumber);
        return formattedNumber;
    }
}
