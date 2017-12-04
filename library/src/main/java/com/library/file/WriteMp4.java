package com.library.file;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import com.library.util.mLog;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/10/20.
 */

public class WriteMp4 {
    private MediaMuxer mMediaMuxer = null;
    public static final int video = 0;
    public static final int voice = 1;

    private final String dirpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "VideoLive";
    private String path = null;
    private MediaFormat videoFormat = null;
    private MediaFormat voiceFormat = null;

    private writeCallback writeCallback;

    private int videoTrackIndex;
    private int voiceTrackIndex;
    private long presentationTimeUsVD = 0;
    private long presentationTimeUsVE = 0;

    private boolean agreeWrite = false;
    private boolean isReady = false;

    private boolean shouldDestroy = false;
    private boolean isCanDestroy = false;

    private boolean isCanStar = true;

    private int frameNum = 0;

    public WriteMp4(String path) {
        this.path = path;
    }

    public void addTrack(MediaFormat mediaFormat, int flag) {
        if (flag == video) {
            videoFormat = mediaFormat;
        } else if (flag == voice) {
            voiceFormat = mediaFormat;
        }
        setReady();
    }

    private void setReady() {
        if (videoFormat != null && voiceFormat != null) {
            isReady = true;
            if (writeCallback != null) {
                writeCallback.isReady();
            }
            mLog.log("app_WriteMp4", "文件录制准备就绪----------------");
        }
    }

    public void write(int flag, ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (agreeWrite) {
            if (flag == video) {
                if (bufferInfo.presentationTimeUs > presentationTimeUsVD) {//容错
                    presentationTimeUsVD = bufferInfo.presentationTimeUs;

                    mMediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
                    frameNum++;
                    mLog.log("app_WriteMp4", "写了视频数据----------------" + bufferInfo.flags);
                    if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                        isCanDestroy = true;
                        if (shouldDestroy) {
                            destroy();
                        }
                    }
                }
            } else if (flag == voice) {
                if (bufferInfo.presentationTimeUs > presentationTimeUsVE) {//容错
                    presentationTimeUsVE = bufferInfo.presentationTimeUs;

                    mMediaMuxer.writeSampleData(voiceTrackIndex, outputBuffer, bufferInfo);
                    mLog.log("app_WriteMp4", "写了音频数据----" + bufferInfo.flags);
                }
            }
        }
    }

    public void star() {
        if (isReady) {
            if (isCanStar) {
                setPath();
                try {
                    mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    videoTrackIndex = mMediaMuxer.addTrack(videoFormat);
                    voiceTrackIndex = mMediaMuxer.addTrack(voiceFormat);
                    mMediaMuxer.start();
                    presentationTimeUsVE = 0;
                    presentationTimeUsVD = 0;
                    frameNum = 0;
                    agreeWrite = true;
                    isCanStar = false;
                    isCanDestroy = false;
                    shouldDestroy = false;
                    if (writeCallback != null) {
                        writeCallback.isStar();
                    }
                    mLog.log("app_WriteMp4", "启动");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                shouldDestroy = false;
            }
        }
    }

    private void setPath() {
        if (path == null) {
            File dirfile = new File(dirpath);
            if (!dirfile.exists()) {
                dirfile.mkdirs();
            }
            path = dirpath + File.separator + System.currentTimeMillis() + ".mp4";
        } else {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public void destroy() {
        if (agreeWrite) {
            if (isCanDestroy) {
                agreeWrite = false;
                mMediaMuxer.release();
                mMediaMuxer = null;
                if (writeCallback != null) {
                    writeCallback.isDestroy();
                }
                mLog.log("app_WriteMp4", "关闭");
                isCanStar = true;
                //文件过短删除
                if (frameNum < 20) {
                    new File(path).delete();
                    if (writeCallback != null) {
                        writeCallback.fileShort();
                    }
                }
            } else {
                shouldDestroy = true;
            }
        }
    }

    public void setWriteCallback(WriteMp4.writeCallback writeCallback) {
        this.writeCallback = writeCallback;
        if (isReady) {
            writeCallback.isReady();
        }
    }

    public interface writeCallback {
        void isReady();

        void isStar();

        void isDestroy();

        void fileShort();
    }
}
