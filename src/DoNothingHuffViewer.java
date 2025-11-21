public class DoNothingHuffViewer implements IHuffViewer {

    @Override
    public void update(String s) {
        // no-op
    }

    @Override
    public void showError(String s) {
        System.out.println("ERROR: " + s);
    }

    @Override
    public void showMessage(String s) {
        System.out.println("MESSAGE: " + s);
    }

    @Override
    public void setModel(IHuffProcessor proc) {
        // no-op
    }
}