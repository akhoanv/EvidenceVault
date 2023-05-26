package com.cork.io.fragment.notepreset;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.cork.io.R;
import com.cork.io.dao.ChecklistNoteData;
import com.cork.io.dao.Note;
import com.cork.io.data.NoteManager;
import com.cork.io.data.ObjectBoxNoteChecklistDataManager;
import com.cork.io.data.ObjectBoxNoteManager;
import com.cork.io.struct.ChecklistItem;
import com.cork.io.struct.ChecklistSortOrder;
import com.cork.io.struct.ElementColor;
import com.cork.io.utils.IntentRequestCode;
import com.google.android.flexbox.FlexboxLayout;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class NoteEditSummaryChecklistFragment extends Fragment implements INoteEditSummaryFragment {
    // Database manager
    private NoteManager noteManager;
    private ObjectBoxNoteChecklistDataManager dataManager;

    private View view;
    private Note note;

    private TextView idElement;
    private ImageView iconElement;
    private EditText titleElement;
    private FlexboxLayout listElement;
    private LinearLayout addBtn;
    private LinearLayout sortBtn;
    private TextView sortBtnText;

    public NoteEditSummaryChecklistFragment(Note note) {
        this.note = note;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        noteManager = ObjectBoxNoteManager.get();
        dataManager = ObjectBoxNoteChecklistDataManager.get();
        view = inflater.inflate(R.layout.fragment_note_summary_checklist, container, false);

        // Find elements
        idElement = view.findViewById(R.id.note_edit_id);
        iconElement = view.findViewById(R.id.note_edit_icon);
        titleElement = view.findViewById(R.id.note_edit_title);
        listElement = view.findViewById(R.id.note_edit_checklist_item_box);
        addBtn = view.findViewById(R.id.note_edit_new_checklist_item_btn);
        sortBtn = view.findViewById(R.id.note_edit_sort_checklist_btn);
        sortBtnText = view.findViewById(R.id.note_edit_sort_checklist_text);

        ChecklistNoteData data = dataManager.findById(note.dataId);

        // Assign appropriate data
        idElement.setText("Note #" + note.getDisplayId());
        titleElement.setText(note.title);
        sortBtnText.setText(getSortLabel(data.order));

        if (note.customIconPath.isEmpty()) {
            iconElement.setImageResource(note.type.getIcon().getId());
        } else {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(Uri.parse(note.customIconPath));
                Bitmap importedImg = BitmapFactory.decodeStream(new BufferedInputStream(inputStream));
                iconElement.setImageBitmap(importedImg);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Set onChangeListener to update the database
        addBtn.setOnClickListener(view1 -> {
            FragmentTransaction ft = getParentFragment().getChildFragmentManager().beginTransaction();
            ft.replace(R.id.note_edit_content_container, new NoteEditSummaryChecklistAddFragment(note));
            ft.commit();
        });

        sortBtn.setOnClickListener(view1 -> {
            ChecklistNoteData updatedData = dataManager.findById(note.dataId);
            updatedData.order = updatedData.order.next();

            sortBtnText.setText(getSortLabel(updatedData.order));
            renderBySorting(updatedData, true);
        });

        iconElement.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, IntentRequestCode.IMAGE_PICKER.ordinal());
        });

        iconElement.setOnLongClickListener(view -> {
            iconElement.setImageResource(note.type.getIcon().getId());
            note.customIconPath = "";
            noteManager.updateNote(note);

            return true;
        });

        // Render data
        renderBySorting(data, false);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null){
            return;
        }

        if (requestCode == IntentRequestCode.IMAGE_PICKER.ordinal()) {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(data.getData());
                Bitmap importedImg = BitmapFactory.decodeStream(new BufferedInputStream(inputStream));
                iconElement.setImageBitmap(importedImg);

                note.customIconPath = data.getData().toString();
                noteManager.updateNote(note);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        // Set these listener to null, avoid mem leak
        addBtn.setOnClickListener(null);
        sortBtn.setOnClickListener(null);
        iconElement.setOnClickListener(null);
        iconElement.setOnLongClickListener(null);

        super.onDestroy();
    }

    private void drawList(List<ChecklistItem> items) {
        listElement.removeAllViews();
        float scale = getResources().getDisplayMetrics().density;

        if (items.size() == 0) {
            return;
        }

        for (ChecklistItem item : items) {
            int paddingVert = (int) (5*scale + 0.5f);
            int paddingHorz = (int) (8*scale + 0.5f);
            int margin = (int) (3*scale + 0.5f);

            TextView listItem = new TextView(getContext());
            listItem.setText(item.getLabel());
            listItem.setTextColor(getContext().getColor(R.color.paper_white));
            listItem.setBackgroundResource(item.getColor().getBackgroundId());
            listItem.setPadding(paddingHorz, paddingVert, paddingHorz, paddingVert);
            if (!item.getStatus()) {
                listItem.setPaintFlags(listItem.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            } else {
                listItem.setPaintFlags(listItem.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            listItem.setOnClickListener(view -> {
                int index = listElement.indexOfChild(listItem);
                ChecklistNoteData updatedData = dataManager.findById(note.dataId);

                if (updatedData.list.get(index).getStatus()) {
                    listItem.setPaintFlags(listItem.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                } else {
                    listItem.setPaintFlags(listItem.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }


                // Update item && re-sort
                updatedData.list.get(index).toggleStatus();
                renderBySorting(updatedData, true);
                dataManager.update(updatedData);
            });

            listItem.setOnLongClickListener(view -> {
                int index = listElement.indexOfChild(listItem);
                ChecklistNoteData updatedData = dataManager.findById(note.dataId);

                // Remove item && re-sort
                updatedData.list.remove(index);
                renderBySorting(updatedData, true);

                dataManager.update(updatedData);

                listElement.removeView(listItem);

                return true;
            });

            listElement.addView(listItem);

            // Add spacing between items
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) listItem.getLayoutParams();
            lp.bottomMargin = margin;
            lp.rightMargin = margin;
        }
    }

    private void renderBySorting(ChecklistNoteData data, boolean updateAfter) {
        switch (data.order) {
            case CHRONOLOGICAL:
                data.list.sort(new ChecklistItem.ChronologicalComparator());
                break;
            case BY_COLOR:
                data.list.sort(new ChecklistItem.ColorComparator());
                break;
            case BY_STATUS:
                data.list.sort(new ChecklistItem.StatusComparator());
                break;
        }

        drawList(data.list);

        if (updateAfter) {
            dataManager.update(data);
        }
    }

    private String getSortLabel(ChecklistSortOrder order) {
        String result = order.name().replace("_", " ").toLowerCase();
        result = result.substring(0, 1).toUpperCase() + result.substring(1);
        return "Sort: " + result;
    }
}