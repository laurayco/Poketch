package io.github.tylerelric.poketch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.Map;

import javax.annotation.Nullable;

import io.github.tylerelric.poketch.data.MapReduceAdapter;
import io.github.tylerelric.poketch.data.QueryViewAdapter;
import io.github.tylerelric.poketch.data.QueryViewHolder;

public class SpeciesViewAdapter
extends QueryViewAdapter{

    public class SpeciesViewHolder extends QueryViewHolder {

        TextView lbl;
        SimpleDraweeView img;
        CardView cv;
        Palette.Builder pbuilder;
        BasePostprocessor img_process;
        ImagePipeline imagePipeline;
        Context cntx;

        public void update_species(Map<String, Object> doc){
            lbl.setText((String) doc.get("name"));
            String url = (String)doc.get("sprite");
            if(!(url instanceof String)) return;
            if(url.contains(".png")) {
                url = "http://pokeapi.co" + url;
            } else {
                url = "http://orig12.deviantart.net/19d6/f/2015/023/b/2/pokeball_wallpaper_1920x1080_by_seyahdoo-d8f1qaw.jpg";
            }
            Uri uri = Uri.parse(url);
            ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(uri);
            ImageRequest req = builder.build();
            PipelineDraweeController controller = (PipelineDraweeController)
                    Fresco.newDraweeControllerBuilder()
                            .setImageRequest(builder.build())
                            .setOldController(img.getController())
                            .build();
            processImageWithPaletteApi(req, controller);
        }

        private void processImageWithPaletteApi(ImageRequest request, DraweeController controller) {
            DataSource<CloseableReference<CloseableImage>> dataSource =
                    imagePipeline.fetchDecodedImage(request, img.getContext());
            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {

                }

                @Override protected void onNewResultImpl(@Nullable Bitmap bitmap) {
                    if(bitmap==null){
                        return;
                    }
                    Palette.Builder builder = new Palette.Builder(bitmap);
                    final Palette palette = builder.generate();
                    ((Activity)SpeciesViewHolder.this.cntx).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int color = palette.getVibrantColor(0);
                            if (color == 0) {
                                color = palette.getMutedColor(0);
                            }
                            if (color == 0) {
                                color = 0xFF00FFFF;
                            }
                            cv.setCardBackgroundColor(color);
                        }
                    });
                }
            }, CallerThreadExecutor.getInstance());

            img.setController(controller);
        }

        public SpeciesViewHolder(android.view.View itemView,Context c ) {
            super(itemView);//http://pokeapi.co/media/img/1.png
            itemView.setOnClickListener(this);
            lbl = (TextView)itemView.findViewById(R.id.textView);
            img = (SimpleDraweeView)itemView.findViewById(R.id.species_list_image);
            cv = (CardView)itemView.findViewById(R.id.card_view);
            cntx = c;
            imagePipeline = Fresco.getImagePipeline();
            getAdapterPosition();
        }

        @Override
        public void update_data(Map<String,Object> dat) {
            super.update_data(dat);
            update_species(dat);
        }

    }

    @Override
    public QueryViewHolder create_holder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        android.view.View view = inflater.inflate(R.layout.species_listing, parent, false);
        SpeciesViewHolder vh = new SpeciesViewHolder(view,parent.getContext());
        return vh;
    }

    @Override
    public void bind_view_holder(RecyclerView.ViewHolder holder, Map<String, Object> props) {
        ((SpeciesViewHolder)holder).update_data(props);
    }

}
