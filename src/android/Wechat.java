package com.qdc.plugins.weixin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class Wechat extends CordovaPlugin {

	public static final String WXAPPID_PROPERTY_KEY = "wechatappid";

	  public static final String ERROR_WECHAT_NOT_INSTALLED = "未安装微信";
	    public static final String ERROR_INVALID_PARAMETERS = "参数错误";
	    public static final String ERROR_USER_CANCEL = "用户点击取消并返回";
	    public static final String ERROR_AUTH_DENIED = "授权失败";
	    public static final String ERROR_SENT_FAILED = "发送失败";
	    public static final String ERROR_UNSUPPORT = "微信不支持";
	    public static final String ERROR_COMMON = "普通错误类型";
	    public static final String ERROR_UNKNOWN = "未知错误";
	
	    public static final String EXTERNAL_STORAGE_IMAGE_PREFIX = "external://";
	    
	public static final String ERROR_WX_NOT_INSTALLED = "Not installed";
	public static final String ERROR_ARGUMENTS = "Argument Error";

	public static final String KEY_ARG_MESSAGE = "message";
	public static final String KEY_ARG_SCENE = "scene";
	public static final String KEY_ARG_TEXT = "text";
	public static final String KEY_ARG_MESSAGE_TITLE = "title";
	public static final String KEY_ARG_MESSAGE_DESCRIPTION = "description";
	public static final String KEY_ARG_MESSAGE_THUMB = "thumb";
	public static final String KEY_ARG_MESSAGE_MEDIA = "media";
	public static final String KEY_ARG_MESSAGE_MEDIA_TYPE = "type";
	public static final String KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL = "webpageUrl";
	public static final String KEY_ARG_MESSAGE_MEDIA_IMAGE = "image";
	public static final String KEY_ARG_MESSAGE_MEDIA_TEXT = "text";

	public static final String ERR_WECHAT_NOT_INSTALLED = "ERR_WECHAT_NOT_INSTALLED";
	public static final String ERR_INVALID_OPTIONS = "ERR_INVALID_OPTIONS";
	public static final String ERR_UNSUPPORTED_MEDIA_TYPE = "ERR_UNSUPPORTED_MEDIA_TYPE";
	public static final String ERR_USER_CANCEL = "ERR_USER_CANCEL";
	public static final String ERR_AUTH_DENIED = "ERR_AUTH_DENIED";
	public static final String ERR_SENT_FAILED = "ERR_SENT_FAILED";
	public static final String ERR_UNSUPPORT = "ERR_UNSUPPORT";
	public static final String ERR_COMM = "ERR_COMM";
	public static final String ERR_UNKNOWN = "ERR_UNKNOWN";
	public static final String NO_RESULT = "NO_RESULT";

	public static final int TYPE_WX_SHARING_APP = 1;
	public static final int TYPE_WX_SHARING_EMOTION = 2;
	public static final int TYPE_WX_SHARING_FILE = 3;
	public static final int TYPE_WX_SHARING_IMAGE = 4;
	public static final int TYPE_WX_SHARING_MUSIC = 5;
	public static final int TYPE_WX_SHARING_VIDEO = 6;
	public static final int TYPE_WX_SHARING_WEBPAGE = 7;
	public static final int TYPE_WX_SHARING_TEXT = 8;

	public static final int SCENE_SESSION = 0;
	public static final int SCENE_TIMELINE = 1;
	public static final int SCENE_FAVORITE = 2;

	public static IWXAPI wxAPI;
	public static CallbackContext currentCallbackContext;

	/** LOG TAG */
	private static final String LOG_TAG = Wechat.class.getSimpleName();

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {

		if (action.equals("share")) {
			// sharing
			return share(args, callbackContext);
		} else if (action.equals("sendAuthRequest")) {
			return sendAuthRequest(args, callbackContext);
		} else if (action.equals("isWXAppInstalled")) {
			return isInstalled(callbackContext);
		}
		// add test file midofy.
		else if (action.equals("payment")) {
			return wxPay(args, callbackContext);
		}
		return super.execute(action, args, callbackContext);
	}

	protected IWXAPI getWXAPI() {
		if (wxAPI == null) {
			String appId = preferences.getString(WXAPPID_PROPERTY_KEY, "");
			wxAPI = WXAPIFactory.createWXAPI(webView.getContext(), appId, true);
		}

		return wxAPI;
	}

	protected boolean wxPay(JSONArray args, CallbackContext callbackContext)
			throws JSONException {
		LOG.d(LOG_TAG, "WeixinPay#payment.start");

		currentCallbackContext = callbackContext;
		PluginResult pluginResult = new PluginResult(
				PluginResult.Status.NO_RESULT);
		pluginResult.setKeepCallback(true);
		callbackContext.sendPluginResult(pluginResult);

		// 参数检查
		if (args.length() != 1) {
			LOG.e(LOG_TAG, "args is empty", new NullPointerException());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR,
					"args is empty");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}

		final JSONObject params = args.getJSONObject(0);

		final String appid = params.getString("appid");
		if (appid == null || "".equals(appid)) {
			LOG.e(LOG_TAG, "appid is empty", new NullPointerException());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR,
					"appid is empty");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}

		final String noncestr = params.getString("noncestr");
		if (noncestr == null || "".equals(noncestr)) {
			LOG.e(LOG_TAG, "noncestr is empty", new NullPointerException());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR,
					"noncestr is empty");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}

		final String packageValue = params.getString("package");
		if (packageValue == null || "".equals(packageValue)) {
			LOG.e(LOG_TAG, "packageValue is empty", new NullPointerException());
			PluginResult result = new PluginResult(PluginResult.Status.OK,
					"packageValue is empty");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}

		final String partnerid = params.getString("partnerid");
		if (partnerid == null || "".equals(partnerid)) {
			LOG.e(LOG_TAG, "partnerid is empty", new NullPointerException());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR,
					"partnerid is empty");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}

		final String prepayid = params.getString("prepayid");
		if (prepayid == null || "".equals(prepayid)) {
			LOG.e(LOG_TAG, "prepayid is empty", new NullPointerException());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR,
					"prepayid is empty");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}

		final String timestamp = params.getString("timestamp");
		if (timestamp == null || "".equals(timestamp)) {
			LOG.e(LOG_TAG, "timestamp is empty", new NullPointerException());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR,
					"timestamp is empty");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}

		final String sign = params.getString("sign");
		if (sign == null || "".equals(timestamp)) {
			LOG.e(LOG_TAG, "sign is empty", new NullPointerException());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR,
					"sign is empty");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}

		// ////////////////////
		// 请求微信支付
		// ////////////////////
		final IWXAPI api = getWXAPI();
		api.registerApp(preferences.getString(WXAPPID_PROPERTY_KEY, ""));
		// wxAPI = WXAPIFactory.createWXAPI(webView.getContext(), appid, true);
		// wxAPI.registerApp(appid);

		if (!api.isWXAppInstalled()) {
			LOG.e(LOG_TAG, "Wechat is not installed",
					new IllegalAccessException());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR,
					"Wechat is not installed");
			result.setKeepCallback(true);
			currentCallbackContext.sendPluginResult(result);
			return false;
		}
		LOG.d(LOG_TAG, "WeixinPay#payment.end");

		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				PayReq payreq = new PayReq();

				payreq.appId = appid;
				payreq.partnerId = partnerid;
				payreq.prepayId = prepayid;
				payreq.packageValue = packageValue;
				payreq.nonceStr = noncestr;
				payreq.timeStamp = timestamp;
				payreq.sign = sign;

				boolean ret = wxAPI.sendReq(payreq);
				if (!ret) {
					PluginResult result = new PluginResult(
							PluginResult.Status.ERROR,
							"unifiedorder requst failured.");
					result.setKeepCallback(true);
					currentCallbackContext.sendPluginResult(result);
				}
			}
		});
		return true;
	}

	protected boolean sendAuthRequest(JSONArray args,
			CallbackContext callbackContext) {
		final IWXAPI api = getWXAPI();
		api.registerApp(preferences.getString(WXAPPID_PROPERTY_KEY, ""));
		final SendAuth.Req req = new SendAuth.Req();
		req.state = "wechat_auth";

		// check if # of arguments is correct
		if (args.length() > 0) {
			try {
				req.scope = args.getString(0);
			} catch (Exception e) {
				Log.e(Wechat.class.getName(),
						"sendAuthRequest parameter parsing failure", e);
			}
		} else {
			req.scope = "snsapi_userinfo";
		}
		api.sendReq(req);
		currentCallbackContext = callbackContext;

		return true;
	}

	protected boolean share(JSONArray args, CallbackContext callbackContext)
			throws JSONException {
		final IWXAPI api = getWXAPI();

		api.registerApp(preferences.getString(WXAPPID_PROPERTY_KEY, ""));

		// check if installed
		if (!api.isWXAppInstalled()) {
			callbackContext.error(ERROR_WX_NOT_INSTALLED);
			return false;
		}

		// check if # of arguments is correct
		if (args.length() != 1) {
			callbackContext.error(ERROR_ARGUMENTS);
		}

		final JSONObject params = args.getJSONObject(0);
		final SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction(null);

		if (params.has(KEY_ARG_SCENE)) {
			int scene = params.getInt(KEY_ARG_SCENE);
			switch (scene) {
			case SCENE_FAVORITE:
				req.scene = SendMessageToWX.Req.WXSceneFavorite;
				break;
			case SCENE_TIMELINE:
				req.scene = SendMessageToWX.Req.WXSceneTimeline;
				break;
			case SCENE_SESSION:
				req.scene = SendMessageToWX.Req.WXSceneSession;
				break;
			}
		} else {
			req.scene = SendMessageToWX.Req.WXSceneTimeline;
		}

		// run in background
		cordova.getThreadPool().execute(new Runnable() {

			@Override
			public void run() {
				try {
					req.message = buildSharingMessage(params);
				} catch (JSONException e) {
					Log.i("out", "run()error" + e);
					Log.e("Wechat", "Sharing error", e);
				}
				api.sendReq(req);
				Log.i("out", "run()end");
			}
		});

		// save the current callback context
		currentCallbackContext = callbackContext;
		Log.i("out", "plugin-end");
		return true;
	}

	protected boolean isInstalled(CallbackContext callbackContext) {
		final IWXAPI api = getWXAPI();
		api.registerApp(preferences.getString(WXAPPID_PROPERTY_KEY, ""));

		if (!api.isWXAppInstalled()) {
			callbackContext.error(ERROR_WX_NOT_INSTALLED);
			return false;
		} else {
			callbackContext.success("true");
		}
		currentCallbackContext = callbackContext;
		return true;
	}

	protected WXMediaMessage buildSharingMessage(JSONObject params)
			throws JSONException {
		Log.i("out", "build");
		// media parameters
		WXMediaMessage.IMediaObject mediaObject = null;
		WXMediaMessage wxMediaMessage = new WXMediaMessage();

		if (params.has(KEY_ARG_TEXT)) {
			WXTextObject textObject = new WXTextObject();
			textObject.text = params.getString(KEY_ARG_TEXT);
			mediaObject = textObject;
			wxMediaMessage.description = textObject.text;
		} else {
			JSONObject message = params.getJSONObject(KEY_ARG_MESSAGE);
			JSONObject media = message.getJSONObject(KEY_ARG_MESSAGE_MEDIA);

			// check types
			int type = media.has(KEY_ARG_MESSAGE_MEDIA_TYPE) ? media
					.getInt(KEY_ARG_MESSAGE_MEDIA_TYPE)
					: TYPE_WX_SHARING_WEBPAGE;

			// thumbnail
			Bitmap thumbnail = getBitmap(message, KEY_ARG_MESSAGE_THUMB);
			if (thumbnail != null) {
				wxMediaMessage.thumbData = Util.bmpToByteArray(thumbnail, true);
			}

			wxMediaMessage.title = message.getString(KEY_ARG_MESSAGE_TITLE);
			wxMediaMessage.description = message
					.getString(KEY_ARG_MESSAGE_DESCRIPTION);
			if (thumbnail != null) {
				Log.i("out", "build-thumbn");
				wxMediaMessage.thumbData = Util.bmpToByteArray(thumbnail, true);
			}

			switch (type) {
			case TYPE_WX_SHARING_APP:
				break;

			case TYPE_WX_SHARING_EMOTION:
				break;

			case TYPE_WX_SHARING_FILE:
				break;

			case TYPE_WX_SHARING_IMAGE:
				Log.i("out", "build-img");
				mediaObject = new WXImageObject();
				String imgURL;
				if (media.getString(KEY_ARG_MESSAGE_MEDIA_IMAGE).contains(
						"temp:")) {
					String urlPath = media
							.getString(KEY_ARG_MESSAGE_MEDIA_IMAGE);
					String[] aurlPath = urlPath.split("temp:");
					Log.i("out", Environment.getExternalStorageDirectory()
							.getAbsolutePath() + "/img/" + aurlPath[1]);
					imgURL = Environment.getExternalStorageDirectory()
							.getAbsolutePath() + "/img/" + aurlPath[1];
				} else {
					imgURL = media.getString(KEY_ARG_MESSAGE_MEDIA_IMAGE);
				}
				((WXImageObject) mediaObject).setImagePath(imgURL);

			case TYPE_WX_SHARING_MUSIC:
				break;

			case TYPE_WX_SHARING_VIDEO:
				break;

			case TYPE_WX_SHARING_WEBPAGE:
			default:
				mediaObject = new WXWebpageObject();
				((WXWebpageObject) mediaObject).webpageUrl = media
						.getString(KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL);
			}
		}

		wxMediaMessage.mediaObject = mediaObject;
		Log.i("out", "buildend");
		return wxMediaMessage;
	}

	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis())
				: type + System.currentTimeMillis();
	}

	public String bitmaptoString(Bitmap bitmap, int bitmapQuality) {

		// 将Bitmap转换成字符串
		String string = null;
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, bitmapQuality, bStream);
		byte[] bytes = bStream.toByteArray();
		string = Base64.encodeToString(bytes, Base64.DEFAULT);
		return string;
	}

	public byte[] Bitmap2Bytes(Bitmap bm) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		return baos.toByteArray();
	}
	protected String getImageURL(JSONObject message, String key) {
        String url = null;

        try {
            url = message.getString(key);

            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ftp://") || url.startsWith("data:")) {
                return url;
            } else if (url.startsWith(EXTERNAL_STORAGE_IMAGE_PREFIX)) { // external path
                String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                url = externalStoragePath + url.substring(EXTERNAL_STORAGE_IMAGE_PREFIX.length());
            } else if (!url.startsWith("/")) { // relative path
                Context context = cordova.getActivity().getApplicationContext();

                File f = new File(context.getCacheDir() + "/assets/" + url);
                if (!f.exists()) {

                    File parent = f.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }

                    // save it to cache
                    try {
                        InputStream is = context.getAssets().open(url);
                        int size = is.available();
                        byte[] buffer = new byte[size];
                        is.read(buffer);
                        is.close();

                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(buffer);
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                url = f.getAbsolutePath();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return url;
    }
	protected Bitmap getBitmap(JSONObject message, String key) {
		String url = getImageURL(message, key);

		if (url != null) {
			try {
				return BitmapFactory.decodeFile(url);
			} catch (Exception e) {
			}
		}

		return null;
	}
}
