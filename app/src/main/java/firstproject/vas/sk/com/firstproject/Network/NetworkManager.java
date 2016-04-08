package firstproject.vas.sk.com.firstproject.Network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import firstproject.vas.sk.com.firstproject.MyApplication;
import firstproject.vas.sk.com.firstproject.PersistentCookieStore;
import firstproject.vas.sk.com.firstproject.R;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by ListenAndRepeat on 2016. 2. 22..
 */
public class NetworkManager {
    private static NetworkManager instance;

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    OkHttpClient mClient;
    OkHttpClient mClientSC;
    private static final int MAX_CACHE_SIZE = 10 * 1024 * 1024;

    private NetworkManager() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        Context context = MyApplication.getContext();
        File cachefile = new File(context.getExternalCacheDir(), "mycache");
        if (!cachefile.exists()) {
            cachefile.mkdirs();
        }
        Cache cache = new Cache(cachefile, MAX_CACHE_SIZE);
        builder.cache(cache);

        CookieManager cookieManager = new CookieManager(new PersistentCookieStore(context), CookiePolicy.ACCEPT_ALL);
        builder.cookieJar(new JavaNetCookieJar(cookieManager));

        mClientSC = builder.build();

        disableCertificateValidation(context, builder);

        mClient = builder.build();
    }

    static void disableCertificateValidation(Context context, OkHttpClient.Builder builder) {

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = context.getResources().openRawResource(R.raw.site);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, tmf.getTrustManagers(), null);
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            sc.init(null, tmf.getTrustManagers(), null);
            builder.sslSocketFactory(sc.getSocketFactory());
            builder.hostnameVerifier(hv);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void cancelAll() {
        mClient.dispatcher().cancelAll();
    }

    public void cancelTag(Object tag) {
        Dispatcher dispatcher = mClient.dispatcher();
        List<Call> calls = dispatcher.queuedCalls();
        for (Call call : calls) {
            if (call.request().tag().equals(tag)) {
                call.cancel();
            }
        }
        calls = dispatcher.runningCalls();
        for (Call call : calls) {
            if (call.request().tag().equals(tag)) {
                call.cancel();
            }
        }
    }

    public interface OnResultListener<T> {
        public void onSuccess(Request request, T result);

        public void onFailure(Request request, int code, Throwable cause);
    }

    private static final int MESSAGE_SUCCESS = 0;
    private static final int MESSAGE_FAILURE = 1;

    static class NetworkHandler extends Handler {
        public NetworkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            CallbackObject object = (CallbackObject) msg.obj;
            Request request = object.request;
            OnResultListener listener = object.listener;
            switch (msg.what) {
                case MESSAGE_SUCCESS:
                    listener.onSuccess(request, object.result);
                    break;
                case MESSAGE_FAILURE:
                    listener.onFailure(request, -1, object.exception);
                    break;
            }
        }
    }

    Handler mHandler = new NetworkHandler(Looper.getMainLooper());

    static class CallbackObject<T> {
        Request request;
        T result;
        IOException exception;
        OnResultListener<T> listener;
    }

    public void cancelAll(Object tag) {

    }


    public Request smsCertification(Context context) {
        String url = String .format(URL_STORY_MODIFY,pid);

        final CallbackObject<StoryWriteResult> callbackObject = new CallbackObject<StoryWriteResult>();


        FormBody.Builder builder = new FormBody.Builder()
                .add("content", contents);
        if(photo != null){
            builder.add("photo",photo);
        }
        RequestBody requestBody =  builder.build();


        Request request = new Request.Builder().url(url)
                .tag(context)
                .put(requestBody)
                .build();


        callbackObject.request = request;
        callbackObject.listener = listener;

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callbackObject.exception = e;
                Message msg = mHandler.obtainMessage(MESSAGE_FAILURE, callbackObject);
                mHandler.sendMessage(msg);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Gson parser = new Gson();
                StoryWriteResult result = parser.fromJson(response.body().string(),StoryWriteResult.class);
                callbackObject.result = result;
                Message msg = mHandler.obtainMessage(MESSAGE_SUCCESS,callbackObject);
                mHandler.sendMessage(msg);
            }
        });
        return request;
    }

    // Story Function

     // Story modify
     public Request modifyStory(Context context,int pid,String contents,String photo,final OnResultListener<StoryWriteResult> listener) throws UnsupportedEncodingException{
         String url = String .format(URL_STORY_MODIFY,pid);

         final CallbackObject<StoryWriteResult> callbackObject = new CallbackObject<StoryWriteResult>();


         FormBody.Builder builder = new FormBody.Builder()
                 .add("content", contents);
         if(photo != null){
             builder.add("photo",photo);
         }
         RequestBody requestBody =  builder.build();


         Request request = new Request.Builder().url(url)
                 .tag(context)
                 .put(requestBody)
                 .build();


         callbackObject.request = request;
         callbackObject.listener = listener;

         mClient.newCall(request).enqueue(new Callback() {
             @Override
             public void onFailure(Call call, IOException e) {
                 callbackObject.exception = e;
                 Message msg = mHandler.obtainMessage(MESSAGE_FAILURE, callbackObject);
                 mHandler.sendMessage(msg);
             }

             @Override
             public void onResponse(Call call, Response response) throws IOException {
                 Gson parser = new Gson();
                 StoryWriteResult result = parser.fromJson(response.body().string(),StoryWriteResult.class);
                 callbackObject.result = result;
                 Message msg = mHandler.obtainMessage(MESSAGE_SUCCESS,callbackObject);
                 mHandler.sendMessage(msg);
             }
         });
         return request;

     }

    /*
    // Matching Modify
    public Request modifyMatching(Context context, int pid, String title, String contents, int limitPeo, int decidePeo, String photo, final OnResultListener<StoryWriteResult> listener) throws UnsupportedEncodingException{
        String url = String .format(URL_STORY_MODIFY,pid);

        final CallbackObject<StoryWriteResult> callbackObject = new CallbackObject<StoryWriteResult>();

        RequestBody requestBody = new FormBody.Builder()
                .add("content",contents)
                .add("title",title)
                .add("photo",photo)
                .add("limit_people", "" + limitPeo)
                .add("decide_people","" + decidePeo)
                .build();


        Request request = new Request.Builder().url(url)
                .tag(context)
                .put(requestBody)
                .build();


        callbackObject.request = request;
        callbackObject.listener = listener;

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callbackObject.exception = e;
                Message msg = mHandler.obtainMessage(MESSAGE_FAILURE, callbackObject);
                mHandler.sendMessage(msg);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Gson parser = new Gson();
                StoryWriteResult result = parser.fromJson(response.body().string(),StoryWriteResult.class);
                callbackObject.result = result;
                Message msg = mHandler.obtainMessage(MESSAGE_SUCCESS,callbackObject);
                mHandler.sendMessage(msg);
            }
        });
        return request;

    }
*/


    //  Story write
/*
    public Request postStoryWrite(Context context,String title,String content,int limitPeo,int decidePeo,File file,final OnResultListener<StoryWriteResult> listener) throws UnsupportedEncodingException{
        String url = URL_STORY_WRITE;

        final CallbackObject<StoryWriteResult> callbackObject = new CallbackObject<StoryWriteResult>();


        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("content", content)
                .addFormDataPart("limit_people", "" + limitPeo)
                .addFormDataPart("decide_people", "" + decidePeo);
        if (file != null) {
            builder.addFormDataPart("photo", "photo.jpg", RequestBody.create(MEDIA_TYPE, file));
        }

        RequestBody requestBody = builder.build();


        Request request = new Request.Builder().url(url)
                .tag(context)
                .post(requestBody)
                .build();


        callbackObject.request = request;
        callbackObject.listener = listener;

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callbackObject.exception = e;
                Message msg = mHandler.obtainMessage(MESSAGE_FAILURE, callbackObject);
                mHandler.sendMessage(msg);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Gson parser = new Gson();
                StoryWriteResult result = parser.fromJson(response.body().string(),StoryWriteResult.class);
                callbackObject.result = result;
                Message msg = mHandler.obtainMessage(MESSAGE_SUCCESS,callbackObject);
                mHandler.sendMessage(msg);
            }
        });
        return request;

    }

*/
    /*
    // Story Get

    public Request getMyStroyList(Context context,int page,String key,String people,int mid,final OnResultListener<ListDetailResult> listener) throws UnsupportedEncodingException{
        String url = String.format(URL_MY_STROY_CONTENT, page, URLEncoder.encode(key, "utf-8"), URLEncoder.encode(people, "utf-8"),mid);

        final CallbackObject<ListDetailResult> callbackObject = new CallbackObject<ListDetailResult>();

        Request request = new Request.Builder().url(url)
                .tag(context)
                .build();

        callbackObject.request = request;
        callbackObject.listener = listener;
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callbackObject.exception = e;
                Message msg = mHandler.obtainMessage(MESSAGE_FAILURE, callbackObject);
                mHandler.sendMessage(msg);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Gson parser = new Gson();
                String text = response.body().string();

                ListDetailResult result = parser.fromJson(text, ListDetailResult.class);
                callbackObject.result = result;
                Message msg = mHandler.obtainMessage(MESSAGE_SUCCESS, callbackObject);
                mHandler.sendMessage(msg);
            }
        });
        return request;
    }
*/



}
