package net.maxsmr.commonutils.android.gui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import net.maxsmr.commonutils.android.gui.GuiUtils;
import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.data.Pair;
import net.maxsmr.commonutils.graphic.GraphicUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public class MaskClickLayout extends FrameLayout {

    @NotNull
    private final ItemHitObservable itemHitCallbacks = new ItemHitObservable();

    @NotNull
    private final LayoutChangeObservable layoutChangeListeners = new LayoutChangeObservable();

    private ImageView backgroundImageView;
    private ImageView layersImageView;
    private ImageView masksImageView;
    private ClickMask clickMask;

    @NotNull
    private ChoiceMode choiceMode = ChoiceMode.MULTIPLE;

    @NotNull
    private final LinkedHashSet<Item> selectedItems = new LinkedHashSet<>();

    public MaskClickLayout(Context context) {
        super(context);
        init();
    }

    public MaskClickLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskClickLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MaskClickLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (clickMask != null) {
            if (event.getPointerCount() == 1) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    Pair<Integer, Integer> backgroundSize = getBackgroundImageSize();

                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    Point point = correctCoordsBySize(new Point(x, y), new Pair<>(backgroundImageView.getMeasuredWidth(), backgroundImageView.getMeasuredHeight()), new Pair<>(backgroundSize.first, backgroundSize.second));
                    point.set(x = point.x, y = point.y);

//                  List<Drawable> drawables = new ArrayList<>();

                    List<Item> hitItems = new ArrayList<>();

                    for (Item item : clickMask.items) {

                        Bitmap maskBitmap = item.maskedPair.second; // GraphicUtils.createBitmapFromResource(item.maskResId, 1, getContext());

                        if (item.options.scaleToParent) {
                            if (backgroundSize.first > 0 && backgroundSize.second > 0) {
                                Bitmap scaledMaskBitmap = Bitmap.createScaledBitmap(maskBitmap, backgroundSize.first, backgroundSize.second, false);

                                if (scaledMaskBitmap == null) {
                                    throw new RuntimeException("could not create scaled bitmap: " + backgroundSize.first + "x" + backgroundSize.second);
                                }

                                maskBitmap.recycle();
                                maskBitmap = scaledMaskBitmap;
                            }
                        }

                        int color = maskBitmap.getPixel(x, y);
                        color &= ~0xFF000000;

                        if (color == item.maskClickColor) {

                            if (itemHitCallbacks.dispatchItemPreHit(point, item)) {
                                continue;
                            }

                            hitItems.add(item);

//                          Drawable currentDrawable = layersImageView.getDrawable();
//                          final int layersCount = currentDrawable instanceof LayerDrawable ? ((LayerDrawable) currentDrawable).getNumberOfLayers() : 0;

                            if (clickMask.findFirstMatch) {
                                break;
                            }
                        }
//                  maskBitmap.recycle();
                    }

                    if (!hitItems.isEmpty()) {
                        if (!itemHitCallbacks.dispatchItemsHit(point, hitItems)) {
                            for (Item item : hitItems) {
                                if (!isItemSelected(item)) {
                                    setItemSelected(item, true);
                                } else {
                                    toggleItemSelected(item);
                                }
                            }
                        }
                    }

//                LayerDrawable layerDrawable = !drawables.isEmpty() ? new LayerDrawable(drawables.toArray(new Drawable[drawables.size()])) : null;
//                layersImageView.setImageDrawable(layerDrawable);
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        unload();
    }

    public void addItemHitCallback(@NotNull ItemHitCallback c) {
        itemHitCallbacks.registerObserver(c);
    }

    public void removeItemHitCallback(@NotNull ItemHitCallback c) {
        itemHitCallbacks.unregisterObserver(c);
    }

    public void addSelectionChangeListener(@NotNull LayoutChangeListener l) {
        layoutChangeListeners.registerObserver(l);
    }

    public void removeSelectionChangeListener(@NotNull LayoutChangeListener l) {
        layoutChangeListeners.unregisterObserver(l);
    }

    @NotNull
    public static List<String> getTags(@NotNull Collection<Item> items) {
        List<String> tags = new ArrayList<>();
        for (Item item : items) {
            if (item != null) {
                tags.add(item.getTag());
            }
        }
        return tags;
    }

    @NotNull
    public ChoiceMode getChoiceMode() {
        return choiceMode;
    }

    public void setChoiceMode(@NotNull ChoiceMode mode) {
        if (this.choiceMode != mode) {
            this.choiceMode = mode;
            if (isLoaded()) {
                if (this.choiceMode == ChoiceMode.NONE || (this.choiceMode == ChoiceMode.SINGLE && getSelectedItemsCount() > 1))
                    clearSelection();
            }
            layoutChangeListeners.dispatchChoiceModeChanged(choiceMode);
        }
    }

    public int getSelectedItemsCount() {
        return selectedItems.size();
    }

    @NotNull
    public LinkedHashSet<Item> getSelectedItems() {
        return new LinkedHashSet<>(selectedItems);
    }

    @NotNull
    public LinkedHashSet<String> getSelectedItemsTags() {
        return new LinkedHashSet<>(getTags(getSelectedItems()));
    }

    public boolean isItemSelected(Item item) {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        return selectedItems.contains(item);
    }

    public boolean setItemSelected(@NotNull Item item, boolean isSelected) {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        if (choiceMode != ChoiceMode.NONE) {

            boolean isAlreadySelected = isItemSelected(item);
            if (isSelected) {
                if (!isAlreadySelected && choiceMode == ChoiceMode.SINGLE) {
                    selectedItems.clear();
                }
                selectedItems.add(item);
            } else {
                selectedItems.remove(item);
            }

            if (isSelected ^ isAlreadySelected) {

                List<Drawable> drawables = new ArrayList<>();

                switch (choiceMode) {

                    case MULTIPLE:
                        for (Item selectedItem : selectedItems) {
                            drawables.add(new BitmapDrawable(getContext().getResources(), selectedItem.layerPair.second));
                        }
                        break;

                    default:
                        break;
                }

                if (isSelected) {
                    drawables.add(new BitmapDrawable(getContext().getResources(), item.layerPair.second));
                }

                LayerDrawable layerDrawable = !drawables.isEmpty() ? new LayerDrawable(drawables.toArray(new Drawable[drawables.size()])) : null;
                layersImageView.setImageDrawable(layerDrawable);

                layoutChangeListeners.dispatchSelectionChanged(item, isSelected);
            }

            return true;
        }

        return false;
    }

    public boolean setItemsSelected(@Nullable Collection<Item> items, boolean isSelected) {
        boolean success = false;
        if (items != null) {
            success = true;
            for (Item it : items) {
                if (!setItemSelected(it, isSelected)) {
                    success = false;
                }
            }
        }
        return success;
    }

    public boolean toggleItemSelected(@NotNull Item item) {
        return setItemSelected(item, !isItemSelected(item));
    }

    public boolean toggleItemsSelected(@Nullable Collection<Item> items) {
        boolean success = true;
        if (items != null) {
            for (Item it : items) {
                if (!toggleItemSelected(it)) {
                    success = false;
                }
            }
        }
        return success;
    }

    @Nullable
    public Item findItemByTag(@Nullable String tag) {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        for (Item item : clickMask.items) {
            if (item != null && CompareUtils.stringsEqual(item.tag, tag, false)) {
                return item;
            }
        }

        return null;
    }

    @NotNull
    public List<Item> findItemsByTags(@Nullable Collection<String> tags) {
        List<Item> items = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                Item item = findItemByTag(tag);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        return items;
    }


    public boolean isItemSelectedByTag(@Nullable String tag) {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        Item item = findItemByTag(tag);
        return item != null && isItemSelected(item);
    }

    public boolean setItemSelectedByTag(@Nullable String tag, boolean isSelected) {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        Item item = findItemByTag(tag);
        return item != null && setItemSelected(item, isSelected);
    }

    public boolean setItemsSelectedByTags(@Nullable Collection<String> tags, boolean isSelected) {

        boolean success = false;

        if (tags != null) {
            success = true;
            for (String tag : tags) {
                if (!setItemSelectedByTag(tag, isSelected)) {
                    success = false;
                }
            }
        }

        return success;
    }

    public boolean toggleItemSelectedByTag(@Nullable String tag) {
        return setItemSelectedByTag(tag, !isItemSelectedByTag(tag));
    }

    public boolean toggleItemsSelectedByTags(@Nullable List<String> tags) {
        boolean success = true;
        if (tags != null) {
            for (String tag : tags) {
                if (!toggleItemSelectedByTag(tag)) {
                    success = false;
                }
            }
        }
        return success;
    }

    public void clearSelection() {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        if (getSelectedItemsCount() > 0) {
            Iterator<Item> iterator = selectedItems.iterator();
            while (iterator.hasNext()) {
                Item it = iterator.next();
                iterator.remove();
                layoutChangeListeners.dispatchSelectionChanged(it, false);
            }
            layersImageView.setImageDrawable(null);
        }
    }

    @NotNull
    public LinkedHashSet<Item> getItems() {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        return clickMask.getItems();
    }

    @NotNull
    public LinkedHashSet<String> getItemsTags() {
        return new LinkedHashSet<>(getTags(getItems()));
    }


    public void drawMask(int id) {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        for (Item it : clickMask.items) {
            if (it.maskedPair.first == id) {
                LayerDrawable masksDrawable = getMasks();
                List<Drawable> drawables = new ArrayList<>();
                if (masksDrawable != null) {
                    for (int i = 0; i < masksDrawable.getNumberOfLayers(); i++) {
                        drawables.add(masksDrawable.getDrawable(i));
                    }
                }
                drawables.add(new BitmapDrawable(getContext().getResources(), it.maskedPair.second));
                masksDrawable = new LayerDrawable(drawables.toArray(new Drawable[drawables.size()]));
                masksImageView.setImageDrawable(masksDrawable);
            }
        }
    }

    public void clearAllMasks() {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        masksImageView.setImageDrawable(null);
    }

    public void drawAllMasks() {

        if (!isLoaded()) {
            throw new IllegalStateException("clickMask was not loaded");
        }

        List<Drawable> drawables = new ArrayList<>();
        for (Item it : clickMask.items) {
            drawables.add(new BitmapDrawable(getContext().getResources(), it.maskedPair.second));
        }
        LayerDrawable masksDrawable = new LayerDrawable(drawables.toArray(new Drawable[drawables.size()]));
        masksImageView.setImageDrawable(masksDrawable);
    }


    public void setBackgroundVisibility(boolean visibility) {
        backgroundImageView.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    public void setMasksVisibility(boolean visibility) {
        masksImageView.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    public void setLayersVisibility(boolean visibility) {
        layersImageView.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    @Nullable
    public Drawable getBackground() {
        return backgroundImageView.getDrawable();
    }

    @Nullable
    public LayerDrawable getLayers() {
        Drawable layers = layersImageView.getDrawable();
        return layers instanceof LayerDrawable ? (LayerDrawable) layersImageView.getDrawable() : null;
    }

    @Nullable
    public LayerDrawable getMasks() {
        Drawable layers = masksImageView.getDrawable();
        return layers instanceof LayerDrawable ? (LayerDrawable) masksImageView.getDrawable() : null;
    }

    @NotNull
    public Pair<Integer, Integer> getBackgroundImageSize() {
        return GuiUtils.getImageViewDrawableSize(backgroundImageView);
    }

    @NotNull
    public Pair<Integer, Integer> getLayersImageSize() {
        return GuiUtils.getImageViewDrawableSize(layersImageView);
    }

    @NotNull
    public Pair<Integer, Integer> getMasksImageSize() {
        return GuiUtils.getImageViewDrawableSize(masksImageView);
    }

    @NotNull
    public Pair<Integer, Integer> getBackgroundRescaledSize() {
        return GuiUtils.getRescaledImageViewSize(backgroundImageView);
    }

    @NotNull
    public Pair<Integer, Integer> getLayersRescaledSize() {
        return GuiUtils.getRescaledImageViewSize(layersImageView);
    }

    @NotNull
    public Pair<Integer, Integer> getMasksRescaledSize() {
        return GuiUtils.getRescaledImageViewSize(masksImageView);
    }

    public boolean isLoaded() {
        return clickMask != null;
    }

    @Nullable
    public ClickMask getClickMask() {
        return clickMask;
    }

    public void load(ClickMask clickMask) {

        if (clickMask != null) {

            unload();

            this.clickMask = clickMask;

            Bitmap background = GraphicUtils.createBitmapFromResource(getContext(), clickMask.backgroundResId);

            if (background == null) {
                throw new RuntimeException("background bitmap was not created");
            }

            if (clickMask.options.scaleToParent) {
                DisplayMetrics metrics = new DisplayMetrics();
                ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
                Bitmap scaledBackground = GraphicUtils.createScaledBitmap(background, metrics.widthPixels);
                background.recycle();
                background = scaledBackground;
            }

            if (clickMask.options.rotateAngleCorrect()) {
                background = GraphicUtils.rotateBitmap(background, clickMask.options.rotateAngle);
            }

            if (clickMask.options.mirror) {
                background = GraphicUtils.mirrorBitmap(background);
            }

            this.backgroundImageView.setImageBitmap(background);

//        if (addLayers) {
//            Drawable[] layers = new Drawable[clickMask.items.size()];
//            int i = 0;
//            for (Item item : clickMask.items) {
////                if (item.options.rotateAngleCorrect() || item.options.mirror) {
////                    if (item.options.rotateAngleCorrect()) {
////                        layers[i] = new BitmapDrawable(getContext().getResources(), item.maskedPair.second);
////                    }
////                } else {
////                    layers[i] = ContextCompat.getDrawable(getContext(), clickMask.items.get(i).layerResId);
////                }
//                layers[i] = new BitmapDrawable(getContext().getResources(), item.maskedPair.second);
//                i++;
//            }
//            LayerDrawable layerDrawable = new LayerDrawable(layers);
//            this.layersImageView.setImageDrawable(layerDrawable);
//        }

        }
    }

    public void unload() {

        if (isLoaded()) {

            clearSelection();

            backgroundImageView.setImageDrawable(null);
            layersImageView.setImageDrawable(null);
            masksImageView.setImageDrawable(null);

            for (Item i : clickMask.items) {
                if (!i.layerPair.second.isRecycled()) {
                    i.layerPair.second.recycle();
                }
                if (!i.maskedPair.second.isRecycled()) {
                    i.maskedPair.second.recycle();
                }
            }
            clickMask.items.clear();
            clickMask = null;
        }
    }

    @NotNull
    private Point correctCoordsBySize(@NotNull Point p, @NotNull Pair<Integer, Integer> size, @NotNull Pair<Integer, Integer> targetSize) {
        float scalerX = (float) p.x / (float) size.first;
        float scalerY = (float) p.y / (float) size.second;
        return new Point(Math.round((float) targetSize.first * scalerX), Math.round((float) targetSize.second * scalerY));
    }

    private static void correctImageViewSize(@NotNull ImageView v) {
        Pair<Integer, Integer> viewSize = GuiUtils.getRescaledImageViewSize(v);
        v.setMaxWidth(viewSize.first);
        v.setMaxHeight(viewSize.second);
        v.invalidate();
        v.requestLayout();
    }

    private void init() {
        addView(backgroundImageView = new ImageView(getContext()));
        addView(masksImageView = new ImageView(getContext()));
        addView(layersImageView = new ImageView(getContext()));
    }

    public static class TransformOptions implements Serializable {

        public final boolean scaleToParent;

        public final int rotateAngle;

        public final boolean mirror;

        public TransformOptions(boolean scaleToParent, int rotateAngle, boolean mirror) {
            this.scaleToParent = scaleToParent;
            this.rotateAngle = rotateAngle;
            this.mirror = mirror;
        }

        public boolean rotateAngleCorrect() {
            return rotateAngle > 0 && rotateAngle < 360;
        }

        @Override
        public String toString() {
            return "TransformOptions{" +
                    "scaleToParent=" + scaleToParent +
                    ", rotateAngle=" + rotateAngle +
                    ", mirror=" + mirror +
                    '}';
        }
    }

    public static class Item {

        @Nullable
        private String tag;

        @NotNull
        private Pair<Integer, Bitmap> layerPair;

        @NotNull
        private Pair<Integer, Bitmap> maskedPair;

        @NotNull
        public Pair<Integer, Bitmap> getLayerPair() {
            return new Pair<>(layerPair.first, layerPair.second);
        }

        @NotNull
        public Pair<Integer, Bitmap> getMaskedPair() {
            return new Pair<>(maskedPair.first, maskedPair.second);
        }

        @ColorInt
        public final int maskClickColor;

        @NotNull
        public final TransformOptions options;

        public Item(@NotNull Context context, @Nullable String tag, @DrawableRes int layerResId, @DrawableRes int maskResId, @ColorInt int maskClickColor, @NotNull TransformOptions options) throws RuntimeException {

            this.tag = tag;

            Bitmap layerBitmap = GraphicUtils.createBitmapFromResource(context, layerResId);
            if (options.rotateAngleCorrect()) {
                layerBitmap = GraphicUtils.rotateBitmap(layerBitmap, options.rotateAngle);
            }
            if (options.mirror) {
                layerBitmap = GraphicUtils.mirrorBitmap(layerBitmap);
            }
            if (layerBitmap == null) {
                throw new RuntimeException("layer bitmap was not created");
            }
            this.layerPair = new Pair<>(layerResId, layerBitmap);

            Bitmap maskBitmap = GraphicUtils.createBitmapFromResource(context, maskResId);
            if (options.rotateAngleCorrect()) {
                maskBitmap = GraphicUtils.rotateBitmap(maskBitmap, options.rotateAngle);
            }
            if (options.mirror) {
                maskBitmap = GraphicUtils.mirrorBitmap(maskBitmap);
            }
            if (maskBitmap == null) {
                throw new RuntimeException("mask bitmap was not created");
            }
            this.maskedPair = new Pair<>(maskResId, maskBitmap);

            this.maskClickColor = maskClickColor & ~0xFF000000;
            this.options = options;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item item = (Item) o;

            if (maskClickColor != item.maskClickColor) return false;
            if (tag != null ? !tag.equals(item.tag) : item.tag != null) return false;
            if (!layerPair.equals(item.layerPair)) return false;
            if (!maskedPair.equals(item.maskedPair)) return false;
            return options.equals(item.options);

        }

        @Override
        public int hashCode() {
            int result = tag != null ? tag.hashCode() : 0;
            result = 31 * result + layerPair.hashCode();
            result = 31 * result + maskedPair.hashCode();
            result = 31 * result + maskClickColor;
            result = 31 * result + options.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "tag=" + tag +
                    ", layerPair=" + layerPair +
                    ", maskedPair=" + maskedPair +
                    ", maskClickColor=" + maskClickColor +
                    ", options=" + options +
                    '}';
        }
    }

    public static class ClickMask {

        @DrawableRes
        public final int backgroundResId;

        public final boolean findFirstMatch;

        @NotNull
        public final TransformOptions options;

        @NotNull
        private final LinkedHashSet<Item> items = new LinkedHashSet<>();

        @NotNull
        public LinkedHashSet<Item> getItems() {
            return new LinkedHashSet<>(items);
        }

        public ClickMask(@DrawableRes int backgroundResId, boolean findFirstMatch, @NotNull TransformOptions options, Item... items) {
            this.backgroundResId = backgroundResId;
            this.findFirstMatch = findFirstMatch;
            this.options = options;
            if (items != null) {
                this.items.addAll(Arrays.asList(items));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClickMask clickMask = (ClickMask) o;

            if (backgroundResId != clickMask.backgroundResId) return false;
            if (findFirstMatch != clickMask.findFirstMatch) return false;
            if (!options.equals(clickMask.options)) return false;
            return items.equals(clickMask.items);

        }

        @Override
        public int hashCode() {
            int result = backgroundResId;
            result = 31 * result + (findFirstMatch ? 1 : 0);
            result = 31 * result + options.hashCode();
            result = 31 * result + items.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ClickMask{" +
                    "backgroundResId=" + backgroundResId +
                    ", findFirstMatch=" + findFirstMatch +
                    ", options=" + options +
                    ", items=" + items +
                    '}';
        }
    }

    public enum ChoiceMode {
        NONE, SINGLE, MULTIPLE
    }

    public interface LayoutChangeListener {

        void onChoiceModeChanged(@NotNull ChoiceMode mode);

        void onSelectedChanged(@NotNull Item item, boolean isSelected);
    }

    public interface ItemHitCallback {

        /**
         * @return true if event consumed
         */
        boolean onItemPreHit(@NotNull Point clickPoint, @NotNull Item item);

        /**
         * @return true if event consumed
         */
        boolean onItemsHit(@NotNull Point clickPoint, @NotNull List<Item> items);
    }

    private static class ItemHitObservable extends Observable<ItemHitCallback> {

        private boolean dispatchItemPreHit(@NotNull Point clickPoint, @NotNull Item item) {
            boolean b = true;
            synchronized (observers) {
                for (ItemHitCallback c : observers) {
                    if (!c.onItemPreHit(clickPoint, item)) {
                        b = false;
                    }
                }
            }
            return b;
        }

        private boolean dispatchItemsHit(@NotNull Point clickPoint, @NotNull List<Item> items) {
            boolean b = true;
            synchronized (observers) {
                for (ItemHitCallback c : observers) {
                    if (!c.onItemsHit(clickPoint, items)) {
                        b = false;
                    }
                }
            }
            return b;
        }
    }

    private static class LayoutChangeObservable extends Observable<LayoutChangeListener> {

        private void dispatchChoiceModeChanged(@NotNull ChoiceMode choiceMode) {
            synchronized (observers) {
                for (LayoutChangeListener l : observers) {
                    l.onChoiceModeChanged(choiceMode);
                }
            }
        }

        private void dispatchSelectionChanged(@NotNull Item item, boolean isSelected) {
            synchronized (observers) {
                for (LayoutChangeListener l : observers) {
                    l.onSelectedChanged(item, isSelected);
                }
            }
        }
    }

}
