package io.github.tylerelric.poketch.io.github.tylerelric.poketch.data;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
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
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;

import android.support.v7.graphics.Palette;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.github.tylerelric.poketch.R;

public class SpeciesAdapter
extends RecyclerView.Adapter
implements LiveQuery.ChangeListener {

    Database database;
    LiveQuery lq;
    QueryEnumerator current_data;
    Context cntx;
    List<Map<String,Object>> last_results;

    public SpeciesAdapter(Database db, com.couchbase.lite.View db_view, Context c) {
        super();
        database = db;
        Query q = db_view.createQuery();
        modify_query(q);
        lq = q.toLiveQuery();
        lq.addChangeListener(this);
        cntx = c;
        Log.i("SpeciesAdapter", "Constructor");
    }

    protected void modify_query(Query q) {
    }

    @Override
    public void changed(LiveQuery.ChangeEvent event) {
        last_results = (List<Map<String, Object>>) event.getRows().next().getValue();
        ((Activity)SpeciesAdapter.this.cntx).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public class SpeciesViewHolder extends RecyclerView.ViewHolder {

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
                url = "http://pokeapi.co/media/img/1.png";
            }
            Uri uri = Uri.parse(url);
            ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(uri);
            ImageRequest req = builder.build();
            PipelineDraweeController controller = (PipelineDraweeController)
                Fresco.newDraweeControllerBuilder()
                    .setImageRequest(builder.build())
                    .setOldController(img.getController())
                    .build();
            processImageWithPaletteApi(req,controller);
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
                    ((Activity)SpeciesAdapter.this.cntx).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cv.setCardBackgroundColor(palette.getVibrantColor(0));
                        }
                    });
                }
            }, CallerThreadExecutor.getInstance());

            img.setController(controller);
        }

        public SpeciesViewHolder(View itemView,Context c ) {
            super(itemView);//http://pokeapi.co/media/img/1.png
            lbl = (TextView)itemView.findViewById(R.id.textView);
            img = (SimpleDraweeView)itemView.findViewById(R.id.species_list_image);
            cv = (CardView)itemView.findViewById(R.id.card_view);
            cntx = c;
            imagePipeline = Fresco.getImagePipeline();
            img_process = new BasePostprocessor() {
                @Override
                public void process(Bitmap destBitmap, Bitmap sourceBitmap) {
                    //super.process(destBitmap, sourceBitmap);
                    pbuilder = new Palette.Builder(sourceBitmap);
                    int col = pbuilder.generate().getVibrantColor(0);
                    cv.setCardBackgroundColor(col);
                    super.process(destBitmap, sourceBitmap);
                }
            };
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        Log.i("SpeciesAdapter", "onAttachedToRecyclerView");
        lq.start();
    }

    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.species_listing, parent, false);
        RecyclerView.ViewHolder vh = new SpeciesViewHolder(view,cntx);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SpeciesViewHolder svh = (SpeciesViewHolder)holder;
        if(holder==null)
            return;
        svh.update_species(last_results.get(position));
    }

    @Override
    public int getItemCount() {
        if(last_results==null){
            return 0;
        }
        return last_results.size();
    }
}
