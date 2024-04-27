package ls_mybatis;

import ls_mybatis.annotation.Id;
import ls_mybatis.annotation.Table;

@Table("tags")
public class Tag {
    private int noteId;
    private int tagId;
    @Id("id")
    private int id;

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
