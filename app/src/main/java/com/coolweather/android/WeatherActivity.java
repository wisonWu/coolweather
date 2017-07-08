package com.coolweather.android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

	private ScrollView mWeatherLayout;
	private TextView mTitleCity;
	private TextView mTitleUpdateTime;
	private TextView mDegreeText;
	private TextView mWeatherInfoText;
	private LinearLayout mForecastLayout;
	private TextView mAqiText;
	private TextView mPm25Text;
	private TextView mComfortText;
	private TextView mCarWashText;
	private TextView mSportText;
	private ImageView mBingPicImg;
	public SwipeRefreshLayout mSwipeRefresh;
	public String mWeatherId;
	private Button mNavButton;
	public DrawerLayout mDrawerLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT>=21){
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
			getWindow().setStatusBarColor(Color.TRANSPARENT);
		}

		setContentView(R.layout.activity_weather);
		//初始化各种控件
		mWeatherLayout = (ScrollView) findViewById(R.id.weather_layout);
		mTitleCity = (TextView) findViewById(R.id.title_city);
		mTitleUpdateTime = (TextView) findViewById(R.id.title_update_time);
		mDegreeText = (TextView) findViewById(R.id.degree_text);
		mWeatherInfoText = (TextView) findViewById(R.id.weather_info_text);
		mForecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
		mAqiText = (TextView) findViewById(R.id.aqi_text);
		mPm25Text = (TextView) findViewById(R.id.pm25_text);
		mComfortText = (TextView) findViewById(R.id.comfort_text);
		mCarWashText = (TextView) findViewById(R.id.car_wash_text);
		mSportText = (TextView) findViewById(R.id.sport_text);
		mBingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
		mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
		mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
		mDrawerLayout= (DrawerLayout) findViewById(R.id.drawer_layout);
		mNavButton = (Button) findViewById(R.id.nav_button);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String weatherString = prefs.getString("weather", null);
		if (weatherString != null) {
			//有缓存的时候直接解析天气数据
			Weather weather = Utility.handleWeatherResponse(weatherString);
			mWeatherId = weather.basic.weatherId;
			showWeatherInfo(weather);
		} else {
			//没有缓存的时候通过服务器查询
			mWeatherId=getIntent().getStringExtra("weather_id");
			mWeatherLayout.setVisibility(View.INVISIBLE);
			requestWeather(mWeatherId);
		}
		mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				requestWeather(mWeatherId);
			}
		});

		String bingPic = prefs.getString("bing_pic", null);
		if (bingPic!=null){
			Glide.with(this).load(bingPic).into(mBingPicImg);
		}else{
			loadBingPic();
		}

		mNavButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				mDrawerLayout.openDrawer(GravityCompat.START);
			}
		});
	}



	public void requestWeather(final String weatherId) {
		String address = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=7ff755467aad4690a74779fa705586c8";
		HttpUtil.sendOkHttpRequest(address, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
						mSwipeRefresh.setRefreshing(false);
					}
				});
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				final String responseText = response.body().string();
				final Weather weather = Utility.handleWeatherResponse(responseText);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (weather != null && "ok".equals(weather.status)) {
							SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
							editor.putString("weather", responseText);
							editor.apply();
							showWeatherInfo(weather);
						} else {
							Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
						}
						mSwipeRefresh.setRefreshing(false);
					}
				});
			}
		});
		loadBingPic();
	}

	/**
	 * 加载必应每日一图
	 */
	private void loadBingPic() {
		String requestBingPic="http://guolin.tech/api/bing_pic";
		HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				final String bingPic = response.body().string();
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
				editor.putString("bing_pic",bingPic);
				editor.apply();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Glide.with(WeatherActivity.this).load(bingPic).into(mBingPicImg);
					}
				});
			}
		});
	}

	private void showWeatherInfo(Weather weather) {
		String cityName = weather.basic.cityName;
		String updateTime = weather.basic.update.updateTime.split(" ")[1];
		String degree = weather.now.temperature + "℃";
		String weatherInfo = weather.now.more.info;
		mTitleCity.setText(cityName);
		mTitleUpdateTime.setText(updateTime);
		mDegreeText.setText(degree);
		mWeatherInfoText.setText(weatherInfo);
		mForecastLayout.removeAllViews();
		for (Forecast forecast : weather.forecastList) {
			//注意空指针异常,这里的每一天都是不同的,数组形式的情况下,不要提取TextView之类的控件为成员变量.
			View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mForecastLayout, false);
			TextView dateText = (TextView) view.findViewById(R.id.date_text);
			TextView infoText = (TextView) view.findViewById(R.id.info_text);
			TextView maxText = (TextView) view.findViewById(R.id.max_text);
			TextView minText = (TextView) view.findViewById(R.id.min_text);
			dateText.setText(forecast.date);
			infoText.setText(forecast.more.info);
			maxText.setText(forecast.temperature.max);
			minText.setText(forecast.temperature.min);
			mForecastLayout.addView(view);
		}
		if (weather.aqi!=null){
			mAqiText.setText(weather.aqi.city.aqi);
			mPm25Text.setText(weather.aqi.city.pm25);
		}
		String comfort="舒适度:"+weather.suggestion.comfort.info;
		String carWash="洗车指数:"+weather.suggestion.carWash.info;
		String sport="舒适度:"+weather.suggestion.sport.info;
		mComfortText.setText(comfort);
		mCarWashText.setText(carWash);
		mSportText.setText(sport);
		mWeatherLayout.setVisibility(View.VISIBLE);

	}


}
