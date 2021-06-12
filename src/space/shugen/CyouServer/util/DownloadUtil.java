package space.shugen.CyouServer.util;

import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;

import java.io.File;
import java.io.IOException;

public class DownloadUtil {

    private static DownloadUtil downloadUtil;
    private final OkHttpClient client;

    public static DownloadUtil get() {
        if (downloadUtil == null) {
            downloadUtil = new DownloadUtil();
        }
        return downloadUtil;
    }

    private DownloadUtil() {
        client = new OkHttpClient();
    }

    /**
     * @param url      下载连接
     * @param listener 下载监听
     */
    public void download(final String url, final String saveDir, final String filename, final OnDownloadListener listener) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                listener.onDownloadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                File downloadedFile = new File(saveDir, filename);
                BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
                sink.writeAll(response.body().source());
                sink.close();
                listener.onDownloadSuccess();
            }
        });
    }


    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess();

        /**
         * 下载失败
         */
        void onDownloadFailed();
    }

    public static void clean() {
        if (downloadUtil != null) {
            downloadUtil.dispose();
        }
    }

    private void dispose() {
        this.client.dispatcher().executorService().shutdownNow();
        this.client.dispatcher().cancelAll();
        this.client.connectionPool().evictAll();
    }
}