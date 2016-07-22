package net.maxsmr.networkutils.loadutil.executors.base.upload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.maxsmr.networkutils.loadutil.executors.base.LoadRunnableInfo;

public class UploadRunnableInfo extends LoadRunnableInfo {

    private static final long serialVersionUID = 2752232594822816968L;

    public UploadRunnableInfo(int id, URL url, @NonNull LoadSettings settings, @Nullable List<Field> headerFields, @Nullable List<Field> formFields, @Nullable FileFormField fileFormField, Integer... acceptableResponseCodes) throws MalformedURLException {
        super(id, UploadRunnableInfo.class.getSimpleName() + "_" + id, url, settings);

        if (headerFields != null) {
            this.headerFields.addAll(headerFields);
        }
        if (formFields != null) {
            this.formFields.addAll(formFields);
        }

        this.fileFormField = fileFormField;

        if (acceptableResponseCodes != null) {
            this.acceptableResponseCodes.addAll(Arrays.asList(acceptableResponseCodes));
        }
    }

    @NonNull
    private final List<Field> headerFields = new ArrayList<>();

    @NonNull
    private final List<Field> formFields = new ArrayList<>();

    @NonNull
    public final List<Field> getHeaderFields() {
        return new ArrayList<>(headerFields);
    }

    @NonNull
    public final List<Field> getFormFields() {
        return new ArrayList<>(formFields);
    }

    @Nullable
    public final FileFormField fileFormField;

    public final List<Integer> acceptableResponseCodes = new ArrayList<>();

    public static class FileFormField {

        @NonNull
        public final File sourceFile;

        @NonNull
        public final String name;

        public final boolean deleteUploadedFile;

        public FileFormField(@NonNull File sourceFile, @NonNull String name, boolean deleteUploadedFile) {

            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("name can't be empty");
            }

            this.sourceFile = sourceFile;
            this.name = name;
            this.deleteUploadedFile = deleteUploadedFile;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FileFormField that = (FileFormField) o;

            if (deleteUploadedFile != that.deleteUploadedFile) return false;
            if (!sourceFile.equals(that.sourceFile)) return false;
            return name.equals(that.name);

        }

        @Override
        public int hashCode() {
            int result = sourceFile.hashCode();
            result = 31 * result + name.hashCode();
            result = 31 * result + (deleteUploadedFile ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "FileFormField{" +
                    "sourceFile=" + sourceFile +
                    ", name='" + name + '\'' +
                    ", deleteUploadedFile=" + deleteUploadedFile +
                    '}';
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        UploadRunnableInfo that = (UploadRunnableInfo) o;

        if (fileFormField != null ? !fileFormField.equals(that.fileFormField) : that.fileFormField != null)
            return false;
        if (!formFields.equals(that.formFields)) return false;
        if (!headerFields.equals(that.headerFields)) return false;
        return !(acceptableResponseCodes != null ? !acceptableResponseCodes.equals(that.acceptableResponseCodes) : that.acceptableResponseCodes != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (fileFormField != null ? fileFormField.hashCode() : 0);
        result = 31 * result + formFields.hashCode();
        result = 31 * result + headerFields.hashCode();
        result = 31 * result + (acceptableResponseCodes != null ? acceptableResponseCodes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UploadRunnableInfo{" +
                "fileFormField=" + fileFormField +
                ", formFields=" + formFields +
                ", headerFields=" + headerFields +
                ", acceptableResponseCodes=" + acceptableResponseCodes +
                ", " + super.toString() +
                '}';
    }
}
