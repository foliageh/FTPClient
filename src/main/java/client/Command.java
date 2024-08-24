package client;

public abstract class Command {
    protected String description = "";

    public Command() {
    }

    public Command(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public abstract void execute();
}