package dk.kb.cfutv.persistence;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.cfutv.GlobalData;
import dk.kb.cfutv.service.CfuTvService;
import dk.kb.cfutv.utils.HarvestTimeSlice;
import dk.kb.cfutv.utils.RitzauHarvestUtil;
import dk.statsbiblioteket.digitaltv.access.model.RitzauProgram;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.GenericHibernateDAO;
import dk.statsbiblioteket.mediaplatform.ingest.model.persistence.HibernateUtilIF;

public class CfuTvDAO extends GenericHibernateDAO<RitzauProgram, Long> {
    private Logger log = LoggerFactory.getLogger(CfuTvDAO.class);
    public CfuTvDAO(HibernateUtilIF util){
        super(RitzauProgram.class, util);
    }

    protected String buildSliceSearchSQL(String channel_name, Timestamp from, Timestamp to, String title, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM mirroredritzauprogram");
        sb.append(" WHERE publishable IS TRUE"); 
        sb.append(" AND channel_name IN (:channels) ");
        sb.append(" AND starttid >= :from");
        sb.append(" AND starttid <= :to");
        
        if(channel_name != null && channel_name.trim().length() != 0){
            sb.append(" AND channel_name = :channel_name");
        }
        
        if(title != null && title.trim().length() != 0){
            sb.append(" AND titel like :title");
        }
        if(description != null && description.trim().length() != 0){
            sb.append(" AND (langomtale1 like :description");
            sb.append(" OR kortomtale like :description");
            sb.append(" OR langomtale2 like :description)");
        }
        
        sb.append(" AND lastupdated = ( SELECT MAX(lastupdated) FROM mirroredritzauprogram");
        sb.append(" WHERE starttid >= :from");
        sb.append(" AND starttid <= :to");
        
        if(channel_name != null && channel_name.trim().length() != 0){
            sb.append(" AND channel_name = :channel_name");
        }
        
        sb.append(" )");
        
        return sb.toString();
    }
    
    protected void addParametersToSliceQuery(SQLQuery query, String channel_name, Timestamp from, Timestamp to, String title,
            String description) {
        query.setParameterList("channels", GlobalData.getAllowedChannels());
        query.setParameter("from", from);
        query.setParameter("to", to);
        
        if(channel_name != null && channel_name.trim().length() != 0){
            query.setParameter("channel_name", channel_name);
        }
        
        if(title != null && title.trim().length() != 0){
            query.setParameter("title", "%" + title + "%");
        }
        if(description != null && description.trim().length() != 0){
            query.setParameter("description", "%" + description + "%");
        }
    }
    
    
    /**
     * Search for RitzauPrograms in the database and return a list of programs matching input data.
     * @param channel_name name for channel (mapped)
     * @param from date
     * @param to date
     * @param title titel
     * @param description Kortomtale, langomtale1 eller langomtale2.
     * @return List of RitzauPrograms matching input data
     */
    @SuppressWarnings("unchecked")
    public List<RitzauProgram> search(String channel_name, ZonedDateTime from, ZonedDateTime to, String title, String description) {
        List<RitzauProgram> programs = new ArrayList<>();

        ZonedDateTime sliceFrom = getEarlyDateLimitation(from);
        ZonedDateTime sliceTo = getLatestDateLimitation(to);
        
        log.debug("Calculated time slices, from: '{}', to: '{}'", sliceFrom, sliceTo);
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(sliceFrom, sliceTo);
        for(HarvestTimeSlice slice : slices) {
            programs.addAll(querySlice(channel_name, slice, title, description));
        }
        return programs;
    }
    
    protected List<RitzauProgram> querySlice(String channel_name, HarvestTimeSlice slice, String title, String description) {
        Session session = null;

        Timestamp fromDate = new Timestamp(slice.getFrom().toInstant().toEpochMilli());
        Timestamp toDate = new Timestamp(slice.getTo().toInstant().toEpochMilli());

        try{
            session = getSession();
            SQLQuery query = session.createSQLQuery(buildSliceSearchSQL(channel_name, fromDate, toDate, title, description));
            addParametersToSliceQuery(query, channel_name, fromDate, toDate, title, description);
            
            List<RitzauProgram> programs = query.addEntity(RitzauProgram.class).list();
                   
            return programs;
        }
        finally {
            if(session != null)
                session.close();
        }
    }
    
    private ZonedDateTime getEarlyDateLimitation(ZonedDateTime from) {
        ZonedDateTime earliestAllowed = GlobalData.getDaysBack();
        log.debug("earliestAllowed gives: '{}'", earliestAllowed);
        if(from == null) {
            return earliestAllowed;
        } else {
            return from.isAfter(earliestAllowed) ? from : earliestAllowed;
        }
    }
    
    private ZonedDateTime getLatestDateLimitation(ZonedDateTime to) {
        ZonedDateTime maxAvailable = RitzauHarvestUtil.getLatestAvailableDate();
        if(to == null) {
            return maxAvailable;
        } else {
            return to.isBefore(maxAvailable) ? to : maxAvailable;
        }
    }
    
    /**
     * Finds and returns a single RitzauProgram based on id.
     * @param id which program.
     * @return Found RitzauProgram.
     */
    public RitzauProgram getByFullId(Long id){
        Session session = null;
        if(id == null) {
        	throw new IllegalStateException("ID was null, should not happen!");
        }
        try{
            session = getSession();
            String sqlQuery = "SELECT * FROM mirroredritzauprogram WHERE starttid >= :starttid AND id = :id";
            SQLQuery query = session.createSQLQuery(sqlQuery);
            
            query.setParameter("starttid", new Date(GlobalData.getDaysBack().toInstant().toEpochMilli()));
            query.setParameter("id", id);

            RitzauProgram program = (RitzauProgram) query.addEntity(RitzauProgram.class).uniqueResult();
            return program;
        }
        finally {
            if(session != null)
                session.close();
        }
    }
}
