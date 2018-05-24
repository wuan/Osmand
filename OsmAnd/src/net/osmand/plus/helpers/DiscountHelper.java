package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;

import net.osmand.AndroidNetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiscountHelper {

	private static final String TAG = "DiscountHelper";
	//private static final String DISCOUNT_JSON = "discount.json";

	private static long mLastCheckTime;
	private static String mTitle;
	private static String mDescription;
	private static String mIcon;
	private static String mUrl;
	private static String mTravelUrl;
	private static String mLang;
	private static boolean mBannerVisible;
	private static final String URL = "https://osmand.net/api/motd";
	private static final String INAPP_PREFIX = "osmand-in-app:";
	private static final String TRAVEL_PREFIX = "travel:";


	public static void checkAndDisplay(final MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		if (settings.DO_NOT_SHOW_STARTUP_MESSAGES.get() || !settings.INAPPS_READ.get()) {
			return;
		}
		if (mBannerVisible) {
			showDiscountBanner(mapActivity, mTitle, mDescription, mIcon, mUrl, mTravelUrl, mLang);
		}
		if (System.currentTimeMillis() - mLastCheckTime < 1000 * 60 * 60 * 24
				|| !settings.isInternetConnectionAvailable()
				|| settings.NO_DISCOUNT_INFO.get()) {
			return;
		}
		mLastCheckTime = System.currentTimeMillis();
		final Map<String, String> pms = new LinkedHashMap<>();
		pms.put("version", Version.getFullVersion(app));
		pms.put("nd", app.getAppInitializer().getFirstInstalledDays() +"");
		pms.put("ns", app.getAppInitializer().getNumberOfStarts() + "");
		pms.put("lang", app.getLanguage() + "");
		try {
			pms.put("aid", Secure.getString(app.getContentResolver(), Secure.ANDROID_ID));
		} catch (Exception e) {
			e.printStackTrace();
		}
		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				try {
					String res = AndroidNetworkUtils.sendRequest(mapActivity.getMyApplication(),
							URL, pms, "Requesting discount info...", false, false);
					return res;
				} catch (Exception e) {
					logError("Requesting discount info error: ", e);
					return null;
				}
			}

			@Override
			protected void onPostExecute(String response) {
				if (response != null) {
					response = "{\n" +
							"  \"message\" : \"Unlimited map downloads -50%!\",\n" +
							"  \"description\" : \"Get Ready for Summer!\",\n" +
							"  \"start\" : \"24-05-2018 00:00\",\n" +
							"  \"end\" : \"29-05-2018 23:59\",\n" +
							"  \"show_start_frequency\" : 300,\n" +
							"  \"show_day_frequency\" : 100,\n" +
							"  \"max_total_show\" : 100,\n" +
							"  \"icon\" : \"ic_action_gift\",\n" +
							"  \"url\" : \"osmand-in-app:osmand_full_version_price\",\n" +
							"  \"url_1\" :  \"osmand-market-app:net.osmand.plus\",\n" +
							"  \"url_2\" : \"osmand-in-app:osmand_full_version_price\",\n" +
							"  \"url_3\" : \"osmand-in-app:osm_live_subscription_2,osm_free_live_subscription_2\",\n" +
							"  \"travel\" : \"travel:World%20Cup%202018\",\n" +
							"  \"lang\" : \"en\",\n" +
							"  \"application\" : {\n" +
							"    \"net.osmand\" : true\n" +
							"  }\n" +
							"}";
					processDiscountResponse(response, mapActivity);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("SimpleDateFormat")
	private static void processDiscountResponse(String response, MapActivity mapActivity) {
		try {
			OsmandApplication app = mapActivity.getMyApplication();

			JSONObject obj = new JSONObject(response);
			String message = obj.getString("message");
			String description = obj.getString("description");
			String icon = obj.getString("icon");
			String url = parseUrl(app, obj.getString("url"));
			String travelUrl = parseUrl(app, obj.getString("travel"));
			String lang = parseUrl(app, obj.getString("lang"));
			SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
			Date start = df.parse(obj.getString("start"));
			Date end = df.parse(obj.getString("end"));
			int showStartFrequency = obj.getInt("show_start_frequency");
			double showDayFrequency = obj.getDouble("show_day_frequency");
			int maxTotalShow = obj.getInt("max_total_show");
			JSONObject application = obj.getJSONObject("application");

//			if (url.startsWith(INAPP_PREFIX) && url.length() > INAPP_PREFIX.length()) {
//				String inAppSku = url.substring(INAPP_PREFIX.length());
//				InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
//				if (purchaseHelper != null
//						&& (purchaseHelper.isPurchased(inAppSku) || InAppPurchaseHelper.isSubscribedToLiveUpdates(app))) {
//					return;
//				}
//			}

			String appName = app.getPackageName();
			Date date = new Date();
//			if (application.has(appName) && application.getBoolean(appName)
//					&& date.after(start) && date.before(end)) {
//
//				OsmandSettings settings = app.getSettings();
//				int discountId = getDiscountId(message, description, start, end);
//				boolean discountChanged = settings.DISCOUNT_ID.get() != discountId;
//				if (discountChanged) {
//					settings.DISCOUNT_TOTAL_SHOW.set(0);
//				}
//				if (discountChanged
//						|| app.getAppInitializer().getNumberOfStarts() - settings.DISCOUNT_SHOW_NUMBER_OF_STARTS.get() >= showStartFrequency
//						|| System.currentTimeMillis() - settings.DISCOUNT_SHOW_DATETIME_MS.get() > 1000L * 60 * 60 * 24 * showDayFrequency) {
//					if(settings.DISCOUNT_TOTAL_SHOW.get() < maxTotalShow){
//						settings.DISCOUNT_ID.set(discountId);
//						settings.DISCOUNT_TOTAL_SHOW.set(settings.DISCOUNT_TOTAL_SHOW.get() + 1);
//						settings.DISCOUNT_SHOW_NUMBER_OF_STARTS.set(app.getAppInitializer().getNumberOfStarts());
//						settings.DISCOUNT_SHOW_DATETIME_MS.set(System.currentTimeMillis());
			showDiscountBanner(mapActivity, message, description, icon, url, travelUrl, lang);
//					}
//				}
//			}
		} catch (Exception e) {
			logError("JSON parsing error: ", e);
		}
	}

	private static String parseUrl(OsmandApplication app, String url) {
		if (!Algorithms.isEmpty(url)) {
			int i = url.indexOf("osmand-market-app:");
			if (i != -1) {
				String appName = url.substring(i + 18);
				return Version.getUrlWithUtmRef(app, appName);
			}
		}
		return url;
	}

	private static int getDiscountId(String message, String description, Date start, Date end) {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		// result = prime * result + ((description == null) ? 0 : description.hashCode());
		// result = prime * result + ((end == null) ? 0 : end.hashCode());
		return result;
	}

	private static void showDiscountBanner(final MapActivity mapActivity, final String title, final String description,
	                                       final String icon, final String url, final String travelUrl, final String lang) {
		final DiscountBarController toolbarController = new DiscountBarController();
		toolbarController.setTitle(title);
		toolbarController.setDescription(description);
		int iconId = mapActivity.getResources().getIdentifier(icon, "drawable", mapActivity.getMyApplication().getPackageName());
		toolbarController.setBackBtnIconIds(iconId, iconId);
		if (!Algorithms.isEmpty(url)) {
			toolbarController.setOnBackButtonClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.getMyApplication().logEvent(mapActivity, "motd_click");
//					mBannerVisible = false;
//					mapActivity.hideTopToolbar(toolbarController);
					openUrl(mapActivity, url, lang);
				}
			});
			toolbarController.setOnTitleClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.getMyApplication().logEvent(mapActivity, "motd_click");
//					mBannerVisible = false;
//					mapActivity.hideTopToolbar(toolbarController);
					openUrl(mapActivity, url, lang);
				}
			});
		}
		toolbarController.setOnCloseButtonClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getMyApplication().logEvent(mapActivity, "motd_close");
//				mBannerVisible = false;
				openUrl(mapActivity, travelUrl, lang);
			}
		});

		mTitle = title;
		mDescription = description;
		mIcon = icon;
		mUrl = url;
		mTravelUrl = travelUrl;
		mLang = lang;
		mBannerVisible = true;

		mapActivity.showTopToolbar(toolbarController);
	}

	private static void openUrl(final MapActivity mapActivity, String url, String lang) {
		if (url.startsWith(INAPP_PREFIX)) {
			if (url.contains(InAppPurchaseHelper.SKU_FULL_VERSION_PRICE)) {
				OsmandApplication app = mapActivity.getMyApplication();
				app.logEvent(mapActivity, "in_app_purchase_redirect");
				InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
				if (purchaseHelper != null) {
					purchaseHelper.purchaseFullVersion(mapActivity);
				}
			} else if (url.contains(InAppPurchaseHelper.SKU_LIVE_UPDATES)){
				ChoosePlanDialogFragment.showOsmLiveInstance(mapActivity.getSupportFragmentManager());
			}
		} else if (url.startsWith(TRAVEL_PREFIX)) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_TEXT, WikiArticleHelper.buildTravelUrl(url, lang));
			intent.setType("text/plain");
			mapActivity.startActivity(Intent.createChooser(intent, mapActivity.getString(R.string.share_article)));
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			mapActivity.startActivity(intent);
		}
	}

	private static class DiscountBarController extends TopToolbarController {

		public DiscountBarController() {
			super(TopToolbarControllerType.DISCOUNT);
			setSingleLineTitle(false);
			setBackBtnIconClrIds(0, 0);
			setCloseBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setDescrTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setBgIds(R.color.discount_bar_bg, R.color.discount_bar_bg,
					R.drawable.discount_bar_bg_land, R.drawable.discount_bar_bg_land);
		}
	}

	private static void logError(String msg) {
		Log.e(TAG, msg);
	}

	private static void logError(String msg, Throwable e) {
		Log.e(TAG, "Error: " + msg, e);
	}
}
