package info.xonix.zlo.search.daemons.impl;

import info.xonix.zlo.search.config.forums.ForumParams;
import info.xonix.zlo.search.config.forums.GetForum;
import info.xonix.zlo.search.index.IndexManager;
import info.xonix.zlo.search.logic.AppLogic;
import info.xonix.zlo.search.logic.IndexerException;
import info.xonix.zlo.search.logic.IndexerLogic;
import info.xonix.zlo.search.logic.exceptions.ExceptionCategory;
import info.xonix.zlo.search.spring.AppSpringContext;
import org.apache.log4j.Logger;

/**
 * Author: Vovan
 * Date: 17.11.2007
 * Time: 19:25:04
 */
public class IndexerDaemon extends BaseSearcherDaemon {
    private static Logger log = Logger.getLogger(IndexerDaemon.class);

    private AppLogic appLogic = AppSpringContext.get(AppLogic.class);
    private IndexerLogic indexerLogic = AppSpringContext.get(IndexerLogic.class);

    public IndexerDaemon(String forumId) {
        this(forumId, GetForum.params(forumId));
    }

    IndexerDaemon(String forumId, ForumParams params) {
        super(forumId,
                params.getIndexerIndexPerTime(),
                params.getIndexerIndexPeriod(),
                params.getIndexerReconnectPeriod());
    }

    protected Logger getLogger() {
        return log;
    }

    @Override
    protected int getFromIndex() {
        return appLogic.getLastIndexedNumber(getForumId());
    }

    @Override
    protected int getEndIndex() {
        return appLogic.getLastSavedMessageNumber(getForumId());
    }

    @Override
    protected void perform(int from, int to) throws IndexerException {
        indexerLogic.index(getForumId(), from, to);
    }

    @Override
    public boolean processException(Exception e) {
        log.error("Exception while indexing", e);

        exceptionsLogger.logException(e,
                "Exception in indexer daemon: " + getForumId(),
                getClass(),
                ExceptionCategory.DAEMON);

        return false;
    }

    public void doOnStart() {
        log.info("Starting indexing to " + getForumId() + " index...");

        // this is for clearing in case of not graceful exit
        log.info("Clearing lock...");
        IndexManager.get(getForumId()).clearLocks();

        super.doOnStart();
    }
}
