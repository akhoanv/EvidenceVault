package com.risky.evidencevault.fragment.board;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.risky.evidencevault.MainActivity;
import com.risky.evidencevault.R;
import com.risky.evidencevault.dao.Board;
import com.risky.evidencevault.dao.Note;
import com.risky.evidencevault.data.ObjectBoxBoardManager;
import com.risky.evidencevault.data.ObjectBoxConnectionManager;
import com.risky.evidencevault.data.ObjectBoxNoteManager;
import com.risky.evidencevault.data.ObjectBoxSettingManager;
import com.risky.evidencevault.data.ObjectBoxTagManager;
import com.risky.evidencevault.struct.ElementColor;
import com.risky.evidencevault.utils.NumberUtil;

public class BoardEditSummaryFragment extends Fragment {
    private ObjectBoxBoardManager boardManager;
    private ObjectBoxNoteManager noteManager;
    private ObjectBoxConnectionManager connectionManager;
    private ObjectBoxTagManager tagManager;
    private ObjectBoxSettingManager settingManager;

    private View view;
    private Board board;
    private boolean doDelete = false;

    private ImageView colorElement;
    private TextView titleElement;
    private TextView idElement;
    private LinearLayout changeBtn;
    private TextView noteNumElement;
    private TextView tagNumElement;
    private LinearLayout deleteBtn;

    public BoardEditSummaryFragment(Board board) {
        this.board = board;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        boardManager = ObjectBoxBoardManager.get();
        noteManager = ObjectBoxNoteManager.get();
        connectionManager = ObjectBoxConnectionManager.get();
        tagManager = ObjectBoxTagManager.get();
        settingManager = ObjectBoxSettingManager.get();
        view = inflater.inflate(R.layout.fragment_board_summary, container, false);

        // Find elements
        colorElement = view.findViewById(R.id.board_edit_color);
        titleElement = view.findViewById(R.id.board_edit_title);
        idElement = view.findViewById(R.id.board_edit_id);
        changeBtn = view.findViewById(R.id.board_edit_change_btn);
        noteNumElement = view.findViewById(R.id.board_edit_note_num);
        tagNumElement = view.findViewById(R.id.board_edit_tag_num);

        // Assign data
        colorElement.setImageResource(board.color.getRoundId());
        titleElement.setText(board.name);
        idElement.setText("Board #" + NumberUtil.convertToDisplayId(board.id));
        noteNumElement.setText(board.notes.size() + "");
        tagNumElement.setText(board.tags.size() + "");

        // Find elements
        deleteBtn = view.findViewById(R.id.board_edit_delete_btn);

        // Assign listener
        colorElement.setOnClickListener(view1 -> {
            ElementColor nextColor = board.color.next();
            colorElement.setImageResource(nextColor.getRoundId());
            board.color = nextColor;
            boardManager.update(board);
        });

        titleElement.setOnFocusChangeListener((view1, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard();

                String enteredTitle = titleElement.getText().toString().trim();
                board.name = enteredTitle.isEmpty() ? "Untitled board" : enteredTitle;
                boardManager.update(board);
            }
        });

        changeBtn.setOnClickListener(view1 -> {
            FragmentTransaction ft = getParentFragment().getChildFragmentManager().beginTransaction();
            ft.replace(R.id.board_edit_content_container, new AllBoardFragment());
            ft.commit();
        });

        deleteBtn.setOnClickListener(view1 -> {
            for (long noteId : board.notes) {
                Note currentNote = noteManager.findById(noteId);
                for (long connId : currentNote.connection) {
                    if (connectionManager.contains(connId)) {
                        connectionManager.remove(connId);
                    }
                }

                noteManager.remove(currentNote.id);
            }

            for (long tagId : board.tags) {
                tagManager.remove(tagId);
            }

            boardManager.remove(board.id);

            ((MainActivity) getContext()).initializeBoard(
                    boardManager.getAll().size() == 0 ? -1 : boardManager.getAll().get(0).id);
            doDelete = true;
            ((DialogFragment) getParentFragment()).dismiss();
        });

        return view;
    }

    @Override
    public void onDestroy() {
        // De-reference listeners to avoid mem leak
        titleElement.setOnFocusChangeListener(null);
        changeBtn.setOnClickListener(null);
        colorElement.setOnClickListener(null);
        deleteBtn.setOnClickListener(null);

        if (!doDelete) {
            String enteredTitle = titleElement.getText().toString().trim();
            board.name = enteredTitle.isEmpty() ? "Untitled board" : enteredTitle;
            boardManager.update(board);

            ((MainActivity) getContext()).initializeBoard(board.id);
            settingManager.setLastVisitedBoard(board.id);
        }

        super.onDestroy();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}