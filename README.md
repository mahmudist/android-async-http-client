#Async Http Client

Callback-based async http client library for Android

##Usage

Init the singleton somewhere on the main thread (just once)

```java
public class ShopaholicApp extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        AsyncHttpClient.init();
    }
}
```
Then use it anywhere you want

```java
Map<String, String> params = new HashMap<String, String>();
params.put("username", username);
params.put("profile-pic", profile_image_url);

AsyncHttpClient.getInstance().post("http://shopaholic-api.herokuapp.com/signup", params, new ResponseHandler() {
    
    @Override
    public void onSuccess(String response) {
        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit()
            .putString("access_token", response.replace("\"", "")).putString("username", username)
            .putString("profile-pic", profile_image_url).commit();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
          
    @Override
    public void onFailure(Exception e) {
        Log.e(TAG, e.getMessage());
    }
});
```

## License

Copyright © 2013 Adel Nizamutdinov
