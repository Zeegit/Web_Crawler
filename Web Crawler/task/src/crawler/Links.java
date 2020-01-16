package crawler;

import java.util.Objects;

public class Links {
    String linkPage;
    String titlePage;
    int depth;

    public Links(String linkPage, String titlePage, int depth) {
        this.titlePage = titlePage;
        this.linkPage = linkPage;
        this.depth = depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Links links = (Links) o;
        return linkPage.equals(links.linkPage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkPage);
    }
}
