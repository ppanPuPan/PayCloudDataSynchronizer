package com.wiseasy.pds.response;

/**
 * Created by Android Studio.
 * User: pupan
 * Date: 8/17/2022
 * Time: 5:09 PM
 * @author pupan
 */
public class TransactionRefundResponse extends  BaseResponse {
    String trans_no;
    int trans_status;

    public String getTrans_no() {
        return trans_no;
    }

    public int getTrans_status() {
        return trans_status;
    }
}
