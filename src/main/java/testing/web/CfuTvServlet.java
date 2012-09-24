package testing.web;

import dk.statsbiblioteket.mediaplatform.ingest.model.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import testing.model.ReducedRitzauProgram;
import testing.service.CfuTvService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: asj
 * Date: 17-08-12
 * Time: 13:29
 * To change this template use File | Settings | File Templates.
 */
@Path("/")
public class CfuTvServlet {
    private Logger log;
    private static CfuTvService service;

    public CfuTvServlet(){
        service = new CfuTvService();
        log = LoggerFactory.getLogger(CfuTvServlet.class);
    }

    /**
     * Searches the db using input and returns xml of the resulting list.
     * Each result has channel name, programId, startTime, endTime, title and a short description.
     * @param channel_name
     * @param fromInput
     * @param toInput
     * @param title
     * @param description
     * @return xml containing a list of results.
     */
    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_XML)
    public List<ReducedRitzauProgram> search(@QueryParam("channelName") String channel_name,
                                             @QueryParam("from") String fromInput,
                                             @QueryParam("to") String toInput,
                                             @QueryParam("title") String title,
                                             @QueryParam("description") String description){
        Date from = null;
        Date to = null;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
        if(fromInput != null && fromInput.trim().length() > 0){
            try{
                from = format.parse(fromInput);
            } catch(ParseException ex){
                log.debug("Date parse error: From: " + fromInput);
                log.debug("Date parse error stacktrace: " + ex.getStackTrace());
            }
        }
        if(toInput != null && toInput.trim().length() > 0){
            try{
                to = format.parse(toInput);
            } catch(ParseException ex){
                log.debug("Date parse error: To: " + toInput);
                log.debug("Date parse error stacktrace: " + ex.getStackTrace());
            }
        }
        if(from != null && to!=null){
            if(from.compareTo(to) > 0){
                throw new InvalidIntervalException("from date " + from + " must be before to date " + to);
            }
        }
        try{
            return service.search(channel_name,from,to,title,description);
        } catch(ServiceException ex){
            throw new WebApplicationException(ex,Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Finds and returns PBCore xml metadata for a single programId.
     * @param programIdRaw
     * @return PBCore xml.
     */
    @GET
    @Path("fullpost")
    @Produces("application/xml")
    public Response fullPost(@QueryParam("id") String programIdRaw){
        if(programIdRaw == null || programIdRaw.trim().length() == 0){
            String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
            result += "<error code=\"400\">Bad Request: No id.</error>";
            return Response.status(400).entity(result).build();
        }
        try{
            Long programId = Long.parseLong(programIdRaw);
            try{
                String result = service.getFullPost(programId);
                log.info("----------------------FULLPOST SUCCESS--------------------");
                return Response.status(200).entity(result).build();
            } catch(ServiceException ex){
                throw new WebApplicationException(ex,Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch(NumberFormatException ex){
            throw new WebApplicationException(ex,Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Finds and uploads a program, based on id and offsets, to the FtpServer as a file with requested filename, along
     * with PBCore xml about the program.
     * Sends back a response with a status code based on whether or not it was successful.
     * Status codes:
     * 200: OK.
     * 400: Bad information in url.
     * 404: Content not available in main archive. Try again later.
     * 409: A file with chosen filename already exists on FtpServer.
     * 410: Content not available.
     * 500: Internal server error or unexpected status code.
     * @param programIdRaw Id of the program.
     * @param filename Wanted filename.
     * @param offsetStartRaw Offset from start of the program in HH:mm:ss format.
     * @param offsetEndRaw Offset from end of the program in HH:mm:ss format.
     * @return Status code in Response and requested file and associated PBCore xml on FtpServer.
     */
    @GET
    @Path("programSnippet")
    @Produces("text/plain")
    public Response programSnippet(@QueryParam("id") String programIdRaw,
                                   @QueryParam("filename") String filename,
                                   @QueryParam("offsetStart") String offsetStartRaw,
                                   @QueryParam("offsetEnd") String offsetEndRaw){
        if(programIdRaw == null || programIdRaw.trim().length() == 0 || filename == null || filename.trim().length() == 0){
            String text = "Bad information in url. Make sure to set id, filename and offsets.";
            log.info("-----------programSnippet first opportunity exit with 400---------------");
            return Response.status(400).entity(text).build();
        }
        Date offsetStart = null;
        Date offsetEnd = null;
        if(offsetStartRaw != null && offsetStartRaw.trim().length() > 0 && offsetEndRaw != null && offsetEndRaw.trim().length() > 0){
            DateFormat format = new SimpleDateFormat("HH:mm:ss");
            try{
                offsetStart = format.parse(offsetStartRaw);
                offsetEnd = format.parse(offsetEndRaw);
            } catch(ParseException ex){
                log.debug("Date parse error: From: " + offsetStartRaw + " | To: " + offsetEndRaw);
                log.debug("Date parse error stacktrace: " + ex.getStackTrace());
                String text = "Failed to parse offsets, make sure it is of following format: HH:mm:ss";
                return Response.status(400).entity(text).build();
            }
        } else {
            String text = "Bad information in url. Make sure to set id, filename and offsets.";
            log.info("-----------programSnippet second opportunity exit with 400---------------");
            return Response.status(400).entity(text).build();
        }
        try{
            Long programId = Long.parseLong(programIdRaw);
            try{
                int statusCode = service.getProgramSnippet(programId, filename, offsetStart, offsetEnd);
                if(statusCode == 200){
                    String text = "OK";
                    log.info("-----------programSnippet exit with 200---------------" + statusCode);
                    return Response.status(statusCode).entity(text).build();
                }
                if(statusCode == 400){
                    String text = "Bad information in url (for instance, unknown channel id)";
                    log.info("-----------programSnippet exit with 400---------------" + statusCode);
                    return Response.status(statusCode).entity(text).build();
                }
                if(statusCode == 404){
                    String text = "Content unavailable on primary archive server, but exists on secondary. Try later.";
                    log.info("-----------programSnippet exit with 404---------------" + statusCode);
                    return Response.status(statusCode).entity(text).build();
                }
                if(statusCode == 409){
                    String text = "A file with that filename already exists.";
                    log.info("-----------programSnippet exit with 409---------------" + statusCode);
                    return Response.status(statusCode).entity(text).build();
                }
                if(statusCode == 410){
                    String text = "Content unavailable, request cannot be fulfilled.";
                    log.info("-----------programSnippet exit with 410---------------" + statusCode);
                    return Response.status(statusCode).entity(text).build();
                }
                log.info("-----------programSnippet exit with 500, unexpected status code---------------" + statusCode);
                return Response.status(500).entity("Unexpected status code.").build(); //Unexpected status code
            } catch(ServiceException ex){
                log.info("-----------programSnippet exit with 500, internal server---------------" + ex.getMessage(), ex);
                return Response.status(500).entity("Internal server error.").build();
            }
        } catch(NumberFormatException ex){
            log.info("-----------programSnippet exit with 400, invalid program id---------------");
            return Response.status(400).entity("Invalid program id, must only contain numbers.").build();
        }
    }

    /**
     * Finds and uploads interval on a channel, based on channel and interval, to the FtpServer as a file with
     * requested filename.
     * Sends back a response with a status code based on whether or not it was successful.
     * Status codes:
     * 200: OK.
     * 400: Bad information in url.
     * 404: Content not available in main archive. Try again later.
     * 409: A file with chosen filename already exists on FtpServer.
     * 410: Content not available.
     * 500: Internal server error or unexpected status code.
     * @param channel Which channel
     * @param filename Wanted filename
     * @param fromRaw from when, format: yyyy-MM-dd_HH:mm:ss
     * @param toRaw to when, format: yyyy-MM-dd_HH:mm:ss
     * @return Status code in Response and requested file on FtpServer.
     */
    @GET
    @Path("rawCut")
    @Produces("text/plain")
    public Response rawCut(@QueryParam("channel") String channel,
                           @QueryParam("filename") String filename,
                           @QueryParam("from") String fromRaw,
                           @QueryParam("to") String toRaw){
        if(channel == null || channel.trim().length() == 0 || filename == null || filename.trim().length() == 0){
            String text = "Bad information in url. Make sure to set channel, filename, from and to.";
            log.info("-----------rawCut first opportunity exit with 400---------------");
            return Response.status(400).entity(text).build();
        }
        Date from = null;
        Date to = null;
        if(fromRaw != null && fromRaw.trim().length() > 0 && toRaw != null && toRaw.trim().length() > 0){
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            try{
                from = format.parse(fromRaw);
                to = format.parse(toRaw);
            } catch(ParseException ex){
                log.debug("Date parse error: From: " + fromRaw + " | To: " + toRaw);
                log.debug("Date parse error stacktrace: " + ex.getStackTrace());
                String text = "Failed to parse dates, make sure it is of following format: yyyy-MM-dd_HH:mm:ss";
                return Response.status(400).entity(text).build();
            }
        } else {
            String text = "Bad information in url. Make sure to set channel, filename, from and to.";
            log.info("-----------rawCut second opportunity exit with 400---------------");
            return Response.status(400).entity(text).build();
        }
        if(from.after(to)){
            return Response.status(400).entity("From must be before To.").build();
        }
        try{
            int statusCode = service.getRawCut(channel,filename,from,to);
            if(statusCode == 200){
                String text = "OK";
                log.info("-----------rawCut exit with 200---------------" + statusCode);
                return Response.status(statusCode).entity(text).build();
            }
            if(statusCode == 400){
                String text = "Bad information in url (for instance, unknown channel id)";
                log.info("-----------rawCut exit with 400---------------" + statusCode);
                return Response.status(statusCode).entity(text).build();
            }
            if(statusCode == 404){
                String text = "Content unavailable on primary archive server, but exists on secondary. Try later.";
                log.info("-----------rawCut exit with 404---------------" + statusCode);
                return Response.status(statusCode).entity(text).build();
            }
            if(statusCode == 409){
                String text = "A file with that filename already exists.";
                log.info("-----------rawCut exit with 409---------------" + statusCode);
                return Response.status(statusCode).entity(text).build();
            }
            if(statusCode == 410){
                String text = "Content unavailable, request cannot be fulfilled.";
                log.info("-----------rawCut exit with 410---------------" + statusCode);
                return Response.status(statusCode).entity(text).build();
            }
            log.info("-----------rawCut exit with 500, unexpected status code---------------" + statusCode);
            return Response.status(500).entity("Unexpected status code.").build(); //Unexpected status code
        } catch(ServiceException ex){
            log.info("-----------rawCut exit with 500, internal server---------------" + ex.getMessage(), ex);
            return Response.status(500).entity("Internal server error.").build();
        }
    }

    /**
     * Returns xml with filename, size and last modification date of file(s) found.
     * Finds specific file if given a filename. If not then it finds all files at download destination.
     * @param filename wanted file, remember extensions.
     * @return Xml with filename, size and last modification date of file(s) found.
     */
    @GET
    @Path("status")
    @Produces("application/xml")
    public Response getStatus(@QueryParam("filename") String filename){
        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        if(filename == null || filename.trim().length() == 0){
            ArrayList<File> files = service.getStatusAll();
            result += "<files>";
            for(File f : files){
                filename = f.getName();
                double size = ((double) f.length()) / 1024;
                long datetime = f.lastModified();
                Date date = new Date(datetime);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String lastModified = simpleDateFormat.format(date);
                result += "<file size=\"" + size + " KB\" lastModified=\"" + lastModified + "\">"+filename+"</file>";
            }
            result += "</files>";
            return Response.status(200).entity(result).build();
        } else {
            File f = service.getStatus(filename);
            if(!f.exists()){
                result += "<error code=\"400\">File not found: " + filename + "</error>";
                return Response.status(400).entity(result).build();
            }
            double size = ((double) f.length()) / 1024;
            long datetime = f.lastModified();
            Date date = new Date(datetime);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String lastModified = simpleDateFormat.format(date);
            result += "<file size=\"" + size + " KB\" lastModified=\"" + lastModified + "\">"+filename+"</file>";
            return Response.status(200).entity(result).build();
        }
    }

    /**
     * Returns current date and time.
     * @return current date and time.
     */
    @GET
    @Path("test")
    @Produces("application/xml")
    public Response getTest(){
        int testVersion = 3;
        String output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        output += "<test><methodCalledTime>" + new Date() + "</methodCalledTime>";
        output += "<version>"+testVersion+"</version></test>";
        log.info("getTest: " + output);
        return Response.status(200).entity(output).build();
    }
}
