package com.risky.jotterbox.worldobject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.risky.jotterbox.MainActivity;
import com.risky.jotterbox.R;
import com.risky.jotterbox.dao.Board;
import com.risky.jotterbox.dao.Note;
import com.risky.jotterbox.data.ObjectBoxBoardManager;
import com.risky.jotterbox.data.ObjectBoxConnectionManager;
import com.risky.jotterbox.data.ObjectBoxNoteManager;
import com.risky.jotterbox.data.ObjectBoxSettingManager;
import com.risky.jotterbox.struct.NoteType;
import com.risky.jotterbox.struct.Point2D;
import com.risky.jotterbox.struct.TouchAction;
import com.risky.jotterbox.utils.DeviceProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Pannable/Zoomable board for main app
 *
 * @author knguyen
 */
public class BoardFragment extends RelativeLayout {
    // Database manager
    private ObjectBoxNoteManager noteManager;
    private ObjectBoxBoardManager boardManager;
    private ObjectBoxConnectionManager connectionManager;
    private ObjectBoxSettingManager settingManager;

    // Stats variable
    private Context context;
    private Point2D onScreenPosition = new Point2D(0, 0);
    private Board board;
    private TouchAction action;

    // Reactive variable
    private Point2D mousePosition = new Point2D(0, 0);
    private float originalDist = 0f;

    public BoardFragment(Context context, long boardId) {
        super(context);
        this.context = context;

        setWillNotDraw(false);

        noteManager = ObjectBoxNoteManager.get();
        boardManager = ObjectBoxBoardManager.get();
        connectionManager = ObjectBoxConnectionManager.get();
        settingManager = ObjectBoxSettingManager.get();

        setBackgroundColor(context.getColor(R.color.board_black));
        setOnTouchListener(touchListener);

        if (boardManager.getAll().size() == 0) {
            board = boardManager.add(new Board());

            // Change UI display stat
            ((MainActivity) context).setCoordDisplay(0, 0);

            // Change setting
            settingManager.setLastVisitedBoard(board.id);
        } else {
            board = boardManager.findById(boardId);
            // Render all child
            for (Long id : board.notes) {
                renderNote(noteManager.findById(id), false);
            }

            onScreenPosition.setXY(board.panPosition.getX(), board.panPosition.getY());

            // Move screen position
            moveChildOnScreen(onScreenPosition);

            // Change UI display stat
            ((MainActivity) context).setCoordDisplay((int) -onScreenPosition.getX(), (int) onScreenPosition.getY());
        }

        ((MainActivity) context).setBoardInfo(board);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Already visited node, prevent drawing duplication
        List<Long> drawnNote = new ArrayList<>();

        // Compare node to node, draw line if requirements are met
        for (int i=0; i < getChildCount(); i++) {
            NoteFragment noteObject = ((NoteFragment) getChildAt(i));
            noteObject.fetchNote();
            Note n = noteObject.getNote();
            for (int i2=0; i2 < getChildCount(); i2++) {
                NoteFragment linkedObject = ((NoteFragment) getChildAt(i2));

                // Only draw if connection hasn't been drawn and connection exists
                if (drawnNote.contains(linkedObject.getNote().id) || !n.getLinkedNotes().contains(linkedObject.getNote().id)) {
                    continue;
                }

                Paint paint = connectionManager.findById(
                        n.getConnectionIdFromLinkedNote(linkedObject.getNote().id)).color.getPaint(context);

                Rect startNoteBound = new Rect();
                noteObject.getHitRect(startNoteBound);

                Rect endNoteBound = new Rect();
                linkedObject.getHitRect(endNoteBound);

                float startX = startNoteBound.left + 140;
                float startY = startNoteBound.top + 40;

                float endX = endNoteBound.left + 140;
                float endY = endNoteBound.top + 40;

                canvas.drawLine(startX, startY, endX, endY, paint);
            }

            drawnNote.add(n.id);
        }
    }

    public void fetchBoard() {
        board = boardManager.findById(board.id);
    }

    public Board getBoard() {
        return board;
    }

    /**
     * Attempting to add a new {@link Note} entry into the database
     */
    public Note addToDatabase(NoteType type) {
        Note note = new Note(board.id, type,
                -board.panPosition.getX() + (DeviceProperties.getScreenWidth() / 3),
                -board.panPosition.getY()  + (DeviceProperties.getScreenHeight() / 3));
        noteManager.add(note);
        board.notes.add(note.id);
        boardManager.update(board);
        return note;
    }

    /**
     * Render note on screen, once the note was successfully added
     */
    public void renderNote(Note note, boolean isNew) {
        NoteFragment noteFragment = new NoteFragment(getContext(), note, isNew);
        addView(noteFragment);
    }

    public Point2D moveTo(final Point2D position) {
        // Current position - self = 0
        // Then minus given position (flipped coord, thus minus)
        Point2D vector = new Point2D(-onScreenPosition.getX() - position.getX(),
                                    -onScreenPosition.getY() - position.getY());

        moveChildOnScreen(vector);
        onScreenPosition.setXY(onScreenPosition.getX() + vector.getX(),
                                onScreenPosition.getY() + vector.getY());

        board.panPosition.setXY(onScreenPosition);
        boardManager.update(board);

        return onScreenPosition;
    }

    /**
     * Move all child elements in one direction
     *
     * @param vector vector amount to move child elements
     */
    private void moveChildOnScreen(final Point2D vector) {
        for (int i=0; i < getChildCount(); i++) {
            ((NoteFragment) getChildAt(i)).move(vector);
        }
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            float newX = motionEvent.getX();
            float newY = motionEvent.getY();

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    // Store initial mouse position
                    mousePosition.setXY(newX, newY);

                    action = TouchAction.DRAG;
                    break;
                case MotionEvent.ACTION_UP:
                    board.panPosition.setXY(onScreenPosition);

                    action = TouchAction.NONE;

                    boardManager.update(board);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (action == TouchAction.DRAG) {
                        float dx = newX - mousePosition.getX();
                        float dy = newY - mousePosition.getY();

                        moveChildOnScreen(new Point2D(dx, dy));
                        onScreenPosition.setXY(onScreenPosition.getX() + dx, onScreenPosition.getY() + dy);
                        board.panPosition.setXY(onScreenPosition);

                        // Update initial mouse position for next move
                        mousePosition.setXY(newX, newY);

                        // Update draw
                        invalidate();

                        // Update screen display
                        ((MainActivity) context).setCoordDisplay((int) -onScreenPosition.getX(), (int) onScreenPosition.getY());
                    }
                    break;
            }
            return true;
        }
    };

    /**
     * Calculate distance between two fingers while zooming
     *
     * @param event motion event
     * @return distance between two fingers
     */
    private float getPinchDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}
