package org.xonix.zlo.search;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexWriter;
import static org.xonix.zlo.search.DAO.DAOException;
import org.xonix.zlo.search.config.Config;
import org.xonix.zlo.search.model.ZloMessage;
import org.xonix.zlo.search.db.DbManager;
import org.xonix.zlo.search.db.DbException;

import java.io.File;
import java.io.IOException;

/**
 * Author: gubarkov
 * Date: 01.06.2007
 * Time: 1:07:38
 */
public class ZloIndexer {
    private static Logger logger = Logger.getLogger(ZloIndexer.class);
    private IndexingSource source;
    private static File INDEX_DIR = new File(Config.INDEX_DIR);

    private static final int INDEX_PER_TIME = 50;

    private IndexWriter _writer;

    public ZloIndexer(IndexingSource source) {
        this(source, false);
    }

    public ZloIndexer(IndexingSource source, boolean reindex) {
        this.source = source;
        if (reindex) {
            logger.info("Clearing index dir...");
            for (File f : INDEX_DIR.listFiles()) {
                if(!f.delete()) {
                    logger.warn("Problems with delting file: " + f.getAbsolutePath());
                }
            }
        }
    }

    public IndexWriter getWriter() {
        if (_writer == null) {
            try {
                _writer = new IndexWriter(INDEX_DIR, ZloMessage.constructAnalyzer(), true);
            } catch (IOException e) {
                logger.error("Can't create writer", e);
            }
        }
        return _writer;
    }

    private void indexRange(int startNum, int endNum) {
        Exception ex = null;
        try {
            IndexWriter writer = getWriter();
            logger.info("Indexing to directory '" + INDEX_DIR + "' range (" + startNum + " - " + endNum + ") ...");
            indexMsgs(startNum, endNum);
            logger.info("Optimizing...");
            writer.optimize();
            writer.close();
        } catch (IOException e) {
            ex = e;
        } catch (DAOException e) {
            ex = e;
        }
        if (ex != null) {
            logger.error("Exception occured: " + ex);
        }
    }

    private void indexMsgs(final int startNum, final int endNum) throws DAOException, IOException {
        int start = startNum, end;
        while (start < endNum) {
            if (start + INDEX_PER_TIME > endNum) {
                end = endNum;
            } else {
                end = start + INDEX_PER_TIME;
            }
            logger.info("Indexing part (" + start + " - " + end + ") ...");
            addMessagesToIndex(start, end);
            start = end;
        }
    }

    private void addMessagesToIndex(int start, int end) throws DAOException, IOException {
        IndexWriter writer = getWriter();
        for (ZloMessage msg : source.getMessages(start, end)) {
            if (msg.getStatus() == ZloMessage.Status.OK) {
                logger.debug("Addind: " +
                        (Config.DEBUG
                                ? msg
                                : msg.getNum()));
                writer.addDocument(msg.getDocument());
            } else {
                logger.debug("Not adding: " + msg.getNum() + " with status: " + msg.getStatus());
            }
        }
    }

    /*
    for use in indexing daemon
    indexes end marks msgs in db as indexed
     */
    public void index(int from, int to) throws IOException, DbException {
        logger.info("Adding msgs (" + from + "-" + to + ") to index...");
        try {
            addMessagesToIndex(from, to);
        } catch (DAOException e) {
            throw new DbException(e.getCause());
        }
        logger.info("Marking msgs (" + from + "-" + to + ") as indexed...");
        DbManager.markRangeAsIndexed(from, to);
    }

    public static void main(String[] args) {
//        new ZloIndexer(DAO.Site.SOURCE).indexRange(3765000, 3765010);
        new ZloIndexer(DAO.DB.SOURCE, true).indexRange(3000000, 4030586);
//        new ZloIndexer(new ZloStorage(), true).indexRange(ZloStorage.FROM, ZloStorage.TO);
    }
}
