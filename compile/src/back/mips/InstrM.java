package back.mips;

import utils.Config;

public class InstrM {
    private Note mipsNote;

    public void setNote(Note note) {
        this.mipsNote = note;
    }

    public String getNote() {
        if (Config.printCommentInMips && mipsNote != null) {
            return new StringBuilder("   ").append(mipsNote.toString()).append("\n").toString();
        }
        return "";
    }
}
