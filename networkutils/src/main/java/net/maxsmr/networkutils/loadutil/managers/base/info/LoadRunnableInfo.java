package net.maxsmr.networkutils.loadutil.managers.base.info;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonElement;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.model.IBuilder;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LoadRunnableInfo<B extends LoadRunnableInfo.Body> extends RunnableInfo {

    private static final long serialVersionUID = 2752232594822816967L;

    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    protected LoadRunnableInfo(Builder<B, ?> b) {
        super(b.id, b.name);
        url = b.url;
        settings = b.settings;
        requestMethod = b.requestMethod;
        contentType = b.contentType;
        acceptableResponseCodes.addAll(b.acceptableResponseCodes);
        headers.addAll(b.headers);
        formFields.addAll(b.formFields);
        body = b.body;
        downloadFile = b.downloadFile;
        downloadDirectory = b.downloadDirectory;
    }

    @NonNull
    public final String url;

    @NonNull
    public final LoadSettings settings;

    @NonNull
    public final RequestMethod requestMethod;

    @NonNull
    public final ContentType contentType;

    @NonNull
    private final List<Integer> acceptableResponseCodes = new ArrayList<>();

    @NonNull
    private final List<NameValuePair> headers = new ArrayList<>();

    /** supports only when multipart or x-www-form-urlencoded */
    @NonNull
    private final List<NameValuePair> formFields = new ArrayList<>();

    @Nullable
    public final B body;

    @Nullable
    public final File downloadFile;

    /** if {@link LoadRunnableInfo#downloadFile} is not specified */
    @Nullable
    public final File downloadDirectory;

    private boolean paused = false;

    @Override
    public synchronized void cancel() {
        super.cancel();
        paused = false;
    }

    @NonNull
    public String getUrlString() {
        return url.toString();
    }

    public boolean hasAcceptableResponseCodes() {
        return !acceptableResponseCodes.isEmpty();
    }

    public List<Integer> getAcceptableResponseCodes() {
        return Collections.unmodifiableList(acceptableResponseCodes);
    }

    public boolean hasHeaderFields() {
        return !headers.isEmpty();
    }

    @NonNull
    public final List<NameValuePair> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public boolean hasFormFields() {
        return !formFields.isEmpty();
    }

    @NonNull
    public final List<NameValuePair> getFormFields() {
        return Collections.unmodifiableList(formFields);
    }

    public boolean hasBody() {
        return body != null;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized void pause() {
        if (!isCanceled()) {
            paused = true;
        }
    }

    public synchronized void resume() {
        if (!isCanceled()) {
            paused = false;
        }
    }

    @Override
    public String toString() {
        return "LoadRunnableInfo{" +
                "url=" + url +
                ", settings=" + settings +
                ", requestMethod=" + requestMethod +
                ", contentType=" + contentType +
                ", acceptableResponseCodes=" + acceptableResponseCodes +
                ", headers=" + headers +
                ", formFields=" + formFields +
                ", body=" + body +
                ", downloadFile=" + downloadFile +
                ", downloadDirectory=" + downloadDirectory +
                ", paused=" + paused +
                '}';
    }

    public static class Builder<B extends Body, I extends LoadRunnableInfo<B>> implements IBuilder<I> {

        private final int id;

        private final String url;

        @NonNull
        private final LoadSettings settings;

        @Nullable
        private String name;

        @NonNull
        private RequestMethod requestMethod = RequestMethod.POST;

        @NonNull
        private ContentType contentType = ContentType.NOT_SPECIFIED;

        @NonNull
        private List<Integer> acceptableResponseCodes = new ArrayList<>();

        @NonNull
        private List<NameValuePair> headers = new ArrayList<>();

        @NonNull
        private List<NameValuePair> formFields = new ArrayList<>();

        @Nullable
        private B body;

        @Nullable
        private File downloadFile;

        @Nullable
        private File downloadDirectory;

        public Builder(int id, String url, @NonNull LoadSettings settings) {
            if (id < 0) {
                throw new IllegalArgumentException("incorrect id: " + id);
            }
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("incorrect url: " + url);
            }
            this.id = id;
            this.url = url;
            this.settings = settings;
            this.name(null);
        }

        public void name(@Nullable String name) {
            this.name = name;
            if (TextUtils.isEmpty(this.name)) {
                this.name = LoadRunnableInfo.class.getSimpleName() + "_" + id;
            }
        }

        public void requestMethod(@NonNull RequestMethod requestMethod) {
            this.requestMethod = requestMethod;
        }

        public void contentType(@NonNull ContentType contentType) {
            this.contentType = contentType;
        }

        public void addAcceptableResponseCodes(@Nullable Collection<Integer> acceptableResponseCodes) {
            if (acceptableResponseCodes != null) {
                this.acceptableResponseCodes.addAll(acceptableResponseCodes);
            }
        }

        public void addAcceptableResponseCodes(Integer... acceptableResponseCodes) {
            if (acceptableResponseCodes != null) {
                addAcceptableResponseCodes(Arrays.asList(acceptableResponseCodes));
            }
        }

        public void addHeaders(@Nullable Collection<NameValuePair> headers) {
            if (headers != null) {
                this.headers.addAll(headers);
            }
        }

        public void addFormFields(@Nullable Collection<NameValuePair> formFields) {
            if (formFields != null) {
                this.formFields.addAll(formFields);
            }
        }

        public void body(@Nullable B body) {
            this.body = body;
        }


        public void downloadFile(@Nullable File downloadFile) {
            this.downloadFile = downloadFile;
        }

        public void downloadDirectory(@Nullable File downloadDirectory) {
            this.downloadDirectory = downloadDirectory;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public I build() throws ClassCastException {
            return (I) new LoadRunnableInfo(this);
        }
    }

    public static class NameValuePair implements Serializable {

        @NonNull
        public final String name;

        @Nullable
        public final String value;

        public NameValuePair(@NonNull String name, @Nullable String value) {

            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("name can't be empty");
            }

            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NameValuePair field = (NameValuePair) o;

            if (!name.equals(field.name)) return false;
            return !(value != null ? !value.equals(field.value) : field.value != null);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Field{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    public static abstract class Body implements Serializable {

        @NonNull
        public final String name;

        /** @param name request optional body parameter name */
        public Body(@NonNull String name) {
            this.name = name;
        }

        public final boolean isEmpty() {
            return getByteCount() == 0;
        }

        public abstract long getByteCount();
    }

    public static class EmptyBody extends Body {

        public EmptyBody(@NonNull String name) {
            super(name);
        }

        @Override
        public long getByteCount() {
            return 0;
        }
    }

    public static class ByteArrayBody extends Body {

        @Nullable
        protected byte[] value;

        public ByteArrayBody(@NonNull String name, @Nullable byte[] value) {
            super(name);
            this.value = value;
        }

        public final long getByteCount() {
            return getBytes().length;
        }

        @NonNull
        public final byte[] getBytes() {
            return value != null? value : new byte[0];
        }

        @NonNull
        public final InputStream openInputStream() {
            return new ByteArrayInputStream(getBytes());
        }
    }

    public static class StringBody extends ByteArrayBody {

        @Nullable
        protected String value;

        public StringBody(@NonNull String name, @Nullable String value) {
            this(name, value, DEFAULT_CHARSET.name());
        }

        public StringBody(@NonNull String name, @Nullable String value, @Nullable String charset) {
            super(name, value != null? value.getBytes(TextUtils.isEmpty(charset)? DEFAULT_CHARSET : Charset.forName(charset)) : null);
            this.value = value;
        }

        @Nullable
        public String getString() {
            return value;
        }

        @Override
        public String toString() {
            return "StringBody{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }

    public static class JsonBody extends StringBody {

        @Nullable
        protected transient JsonElement jsonElement;

        public JsonBody(@NonNull String name, @Nullable JsonElement jsonElement) {
            this(name, jsonElement, DEFAULT_CHARSET.name());
        }

        public JsonBody(@NonNull String name, @Nullable JsonElement jsonElement, @Nullable String charset) {
            super(name, jsonElement != null? jsonElement.toString() : null, charset);
            this.jsonElement = jsonElement;
        }

        @Nullable
        public JsonElement getJsonElement() {
            return jsonElement;
        }

        @Nullable
        @Override
        public String getString() {
            if (value == null && jsonElement != null) {
                value = jsonElement.toString();
            }
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JsonBody jsonBody = (JsonBody) o;

            return jsonElement != null ? jsonElement.equals(jsonBody.jsonElement) : jsonBody.jsonElement == null;
        }

        @Override
        public int hashCode() {
            return jsonElement != null ? jsonElement.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "JsonBody{" +
                    "jsonElement=" + jsonElement +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    public static class FilesBody extends Body {

        @NonNull
        private final Set<File> sourceFiles = new LinkedHashSet<>();

        public final boolean asArray;

        public final boolean ignoreIncorrect;

        /**
         * @param name request optional body parameter name
         */
        public FilesBody(@NonNull String name, @NonNull Collection<File> files, boolean asArray, boolean ignoreIncorrect) {
            super(name);
            this.sourceFiles.addAll(files);
            this.asArray = asArray;
            this.ignoreIncorrect = ignoreIncorrect;
        }

        public boolean hasCorrectSourceFiles() {
            boolean has = false;
            for (File f : sourceFiles) {
                if (FileHelper.isFileCorrect(f)) {
                    has = true;
                    break;
                }
            }
            return has;
        }

        public boolean hasIncorrectSourceFiles() {
            boolean has = false;
            for (File f : sourceFiles) {
                if (!FileHelper.isFileCorrect(f)) {
                    has = true;
                    break;
                }
            }
            return has;
        }

        @Override
        public long getByteCount() {
            int size = 0;
            for (File f : sourceFiles) {
                size += f.length();
            }
            return size;
        }

        @NonNull
        public List<File> getSourceFiles() {
            return new ArrayList<>(sourceFiles);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FilesBody filesBody = (FilesBody) o;

            if (asArray != filesBody.asArray) return false;
            if (ignoreIncorrect != filesBody.ignoreIncorrect) return false;
            return sourceFiles.equals(filesBody.sourceFiles);

        }

        @Override
        public int hashCode() {
            int result = sourceFiles.hashCode();
            result = 31 * result + (asArray ? 1 : 0);
            result = 31 * result + (ignoreIncorrect ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "FilesBody{" +
                    "sourceFiles=" + sourceFiles +
                    ", asArray=" + asArray +
                    ", ignoreIncorrect=" + ignoreIncorrect +
                    '}';
        }
    }

    public static class FileBody extends FilesBody {

        public FileBody(@NonNull String name, @NonNull File sourceFile, boolean asArray, boolean ignoreIncorrect) {
            super(name, Collections.singletonList(sourceFile), asArray, ignoreIncorrect);
        }

        @NonNull
        public File getSourceFile() {
            return getSourceFiles().get(0);
        }

        public boolean isSourceFileCorrect() {
            return !hasIncorrectSourceFiles();
        }
    }

    public final static class LoadSettings implements Serializable {

        /**
         * no retries
         */
        public static final int RETRY_LIMIT_NONE = -1;

        /**
         * infinite load attempts
         */
        public static final int RETRY_LIMIT_UNLIMITED = 0;

        public final long connectionTimeout;

        public final long readWriteTimeout;

        public final int retryLimit;

        public final long retryDelay;

        public final boolean notifyRead;

        public final boolean notifyWrite;

        public final boolean logRequestData;

        public final boolean logResponseData;

        @NonNull
        public final DownloadWriteMode downloadWriteMode;

        /** for download */
        public final boolean allowDeleteDownloadFile;

        /** for upload */
        public final boolean allowDeleteUploadFiles;

        @NonNull
        public final ReadBodyMode readBodyMode;

        public final String uploadCharset;

        public final String downloadCharset;

        public LoadSettings(@NonNull Builder builder) {
            connectionTimeout = builder.connectionTimeout;
            readWriteTimeout = builder.readWriteTimeout;
            retryLimit = builder.retryLimit;
            retryDelay = builder.retryDelay;
            notifyRead = builder.notifyRead;
            notifyWrite = builder.notifyWrite;
            logRequestData = builder.logRequestData;
            logResponseData = builder.logResponseData;
            downloadWriteMode = builder.downloadWriteMode;
            allowDeleteDownloadFile = builder.allowDeleteDownloadFile;
            allowDeleteUploadFiles = builder.allowDeleteUploadFiles;
            readBodyMode = builder.readBodyMode;
            uploadCharset = builder.uploadCharset;
            downloadCharset = builder.downloadCharset;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LoadSettings that = (LoadSettings) o;

            if (connectionTimeout != that.connectionTimeout) return false;
            if (readWriteTimeout != that.readWriteTimeout) return false;
            if (retryLimit != that.retryLimit) return false;
            if (retryDelay != that.retryDelay) return false;
            if (notifyRead != that.notifyRead) return false;
            if (notifyWrite != that.notifyWrite) return false;
            if (logRequestData != that.logRequestData) return false;
            if (logResponseData != that.logResponseData) return false;
            if (allowDeleteDownloadFile != that.allowDeleteDownloadFile) return false;
            if (allowDeleteUploadFiles != that.allowDeleteUploadFiles) return false;
            if (downloadWriteMode != that.downloadWriteMode) return false;
            return readBodyMode == that.readBodyMode;

        }

        @Override
        public int hashCode() {
            int result = (int) (connectionTimeout ^ (connectionTimeout >>> 32));
            result = 31 * result + (int) (readWriteTimeout ^ (readWriteTimeout >>> 32));
            result = 31 * result + retryLimit;
            result = 31 * result + (int) (retryDelay ^ (retryDelay >>> 32));
            result = 31 * result + (notifyRead ? 1 : 0);
            result = 31 * result + (notifyWrite ? 1 : 0);
            result = 31 * result + (logRequestData ? 1 : 0);
            result = 31 * result + (logResponseData ? 1 : 0);
            result = 31 * result + downloadWriteMode.hashCode();
            result = 31 * result + (allowDeleteDownloadFile ? 1 : 0);
            result = 31 * result + (allowDeleteUploadFiles ? 1 : 0);
            result = 31 * result + readBodyMode.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "LoadSettings{" +
                    "connectionTimeout=" + connectionTimeout +
                    ", readWriteTimeout=" + readWriteTimeout +
                    ", retryLimit=" + retryLimit +
                    ", retryDelay=" + retryDelay +
                    ", notifyRead=" + notifyRead +
                    ", notifyWrite=" + notifyWrite +
                    ", logRequestData=" + logRequestData +
                    ", logResponseData=" + logResponseData +
                    ", downloadWriteMode=" + downloadWriteMode +
                    ", allowDeleteDownloadFile=" + allowDeleteDownloadFile +
                    ", allowDeleteUploadFiles=" + allowDeleteUploadFiles +
                    ", readBodyMode=" + readBodyMode +
                    '}';
        }

        public enum ReadBodyMode {
            BYTE_ARRAY, STRING, FILE
        }

        public static final class Builder implements IBuilder<LoadSettings> {

            private long connectionTimeout;

            private long readWriteTimeout;

            private int retryLimit = RETRY_LIMIT_NONE;

            private long retryDelay;

            private boolean notifyRead = true;

            private boolean notifyWrite = true;

            private boolean logRequestData;

            private boolean logResponseData;

            @NonNull
            private DownloadWriteMode downloadWriteMode = DownloadWriteMode.DO_NOTING;

            /** for download */
            private boolean allowDeleteDownloadFile;

            /** for upload */
            private boolean allowDeleteUploadFiles;

            @NonNull
            private ReadBodyMode readBodyMode = ReadBodyMode.STRING;

            @NonNull
            private String uploadCharset = DEFAULT_CHARSET.name();

            @NonNull
            private String downloadCharset = DEFAULT_CHARSET.name();

            public Builder() {
            }

            public Builder connectionTimeout(long connectionTimeout) {
                if (connectionTimeout < 0) {
                    throw new IllegalArgumentException("incorrect connectionTimeout: " + connectionTimeout);
                }
                this.connectionTimeout = connectionTimeout;
                return this;
            }

            public Builder readWriteTimeout(long readWriteTimeout) {
                if (readWriteTimeout < 0) {
                    throw new IllegalArgumentException("incorrect readWriteTimeout: " + readWriteTimeout);
                }
                this.readWriteTimeout = readWriteTimeout;
                return this;
            }

            public Builder retryLimit(int retryLimit) {
                if (retryLimit < 0 && retryLimit != RETRY_LIMIT_NONE) {
                    throw new IllegalArgumentException("incorrect retryLimit: " + retryLimit);
                }
                this.retryLimit = retryLimit;
                return this;
            }

            public Builder retryDelay(long retryDelay) {
                if (retryDelay < 0) {
                    throw new IllegalArgumentException("incorrect retryDelay: " + retryDelay);
                }
                this.retryDelay = retryDelay;
                return this;
            }

            public Builder notifyRead(boolean notifyRead) {
                this.notifyRead = notifyRead;
                return this;
            }

            public Builder notifyWrite(boolean notifyWrite) {
                this.notifyWrite = notifyWrite;
                return this;
            }

            public Builder logRequestData(boolean logRequestData) {
                this.logRequestData = logRequestData;
                return this;
            }

            public Builder logResponseData(boolean logResponseData) {
                this.logResponseData = logResponseData;
                return this;
            }

            public Builder setDownloadWriteMode(@NonNull DownloadWriteMode downloadWriteMode) {
                this.downloadWriteMode = readBodyMode == ReadBodyMode.FILE? downloadWriteMode : DownloadWriteMode.DO_NOTING;
                return this;
            }

            public Builder allowDeleteDownloadFile(boolean allowDeleteDownloadFile) {
                this.allowDeleteDownloadFile = allowDeleteDownloadFile;
                return this;
            }

            public Builder allowDeleteUploadFile(boolean allowDeleteUploadFile) {
                this.allowDeleteUploadFiles = allowDeleteUploadFile;
                return this;
            }

            public Builder readBodyMode(@NonNull ReadBodyMode mode) {
                this.readBodyMode = mode;
                if (this.readBodyMode != ReadBodyMode.FILE) {
                    this.downloadWriteMode = DownloadWriteMode.DO_NOTING;
                }
                return this;
            }

            public void uploadCharset(@NonNull String uploadCharset) {
                this.uploadCharset = uploadCharset;
            }

            public void downloadCharset(@NonNull String downloadCharset) {
                this.downloadCharset = downloadCharset;
            }

            @NonNull
            @Override
            public LoadSettings build() {
                return new LoadSettings(this);
            }
        }

        public enum DownloadWriteMode {
            OVERWRITE, RESUME_DOWNLOAD, CREATE_NEW, DO_NOTING
        }

    }

    public enum RequestMethod {
        GET, POST, PUT, PATCH, HEAD, DELETE
    }

    /** supported content types */
    public enum ContentType {

        NOT_SPECIFIED(""),
        TEXT_PLAIN("text/plain"),
        TEXT_CSS("text/css"),
        TEXT_HTML("text/html"),
        APPLICATION_XML("application/xml"),
        APPLICATION_ATOM_XML("application/atom+xml"),
        APPLICATION_JAVASCRIPT("application/javascript"),
        APPLICATION_JSON("application/json"),
        APPLICATION_URLENCODED("application/x-www-form-urlencoded"),
        MULTIPART_FORM_DATA("multipart/form-data");

        @NonNull
        public final String value;

        ContentType(@NonNull String value) {
            this.value = value;
        }
    }

}
