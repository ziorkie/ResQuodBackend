package com.ustudent.resquod.controller;


import com.ustudent.resquod.exception.*;
import com.ustudent.resquod.model.dao.*;
import com.ustudent.resquod.service.EventService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;


@RestController
@Api(value = "Event Management")
public class EventController {

    private final EventService eventService;


    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @ApiOperation(value = "Returns Admin events List", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 500, message = "Server Error!")})
    @GetMapping(value = "/adminEvents")
    public Set<EventDTO> getAdminEvents() {
        try {
            return eventService.findEventsWhereUserIsAdmin();
        } catch (EmailExistException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        }
    }

    @ApiOperation(value = "Returns user events List", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 500, message = "Server Error!")})
    @GetMapping(value = "/userEvents")
    public Set<EventDTO> getUserEvents() {
        try {
            return eventService.findUserEvents();
        } catch (EmailExistException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        }
    }

    @ApiOperation(value = "Change Event data", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully updated!"),
            @ApiResponse(code = 400, message = "\"Invalid input!\" or \"Invalid Password!\""),
            @ApiResponse(code = 500, message = "Server Error!")})
    @PatchMapping("/eventChanges")
    public ResponseTransfer changeEventData(
            @ApiParam(value = "Required id, name, Event Room id, AdminPassword", required = true)
            @RequestBody EventData userInput) {
        try {
            eventService.changeEventData(userInput);
        } catch (InvalidAdminId ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Permisson!");
        } catch (InvalidInputException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input!");
        } catch (PasswordAlreadyExists ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Try another Password/Key to event!");
        } catch (RoomDoesntBelongToYourCorpo ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You Cannot use this room");
        } catch (ObjectNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Object not Found");
        }
        return new ResponseTransfer("Successfully updated!");
    }


    @ApiOperation(value = "Add New Event", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Event Added Succesfully"),
            @ApiResponse(code = 400, message = "\"Invalid Input\" or \"Event Already Exists\""),
            @ApiResponse(code = 404, message = "Room Does Not Exist"),
            @ApiResponse(code = 401, message = "Permission Denied")})
    @PostMapping("/event")
    public ResponseTransfer addNewEvent(@ApiParam(value = "Required name, password, room id", required = true)
                                        @RequestBody NewEventData newEvent) {
        try {
            eventService.addNewEvent(newEvent);
        } catch (EventAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event Already Exists");
        } catch (RoomNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room Does Not Exist");
        } catch (InvalidInputException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Input");
        } catch (PermissionDeniedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Permission Denied");
        }
        return new ResponseTransfer("Event Added Successfully");
    }

    @ApiOperation(value = "Returns all Events", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 403, message = "You Have no permission"),
            @ApiResponse(code = 500, message = "Server Error!")})
    @GetMapping(value = "/allEvents")
    public List<EventDTO> showAllEvents() {
        try {
            return eventService.showEveryEvent();
        } catch (EmailExistException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        }
    }

    @ApiOperation(value = "Returns Corporation's Events", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Corporation events:"),
            @ApiResponse(code = 401, message = "Permission Denied")})
    @PostMapping(value = "/corpotationsEvents")
    public List<EventData> getCorpoEvents(@ApiParam(value = "Required corporation id", required = true)
                                          @RequestBody CorpoData corpoId) {
        try {
            return eventService.findByCorpoId(corpoId.getId());
        } catch (PermissionDeniedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Permission Denied");
        }
    }

    @ApiOperation(value = "Joining an event", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully joined the event"),
            @ApiResponse(code = 400, message = "\"Wrong password\" or \"Event Does Not Exist\" or \"You already belong to this event\""),
            @ApiResponse(code = 500, message = "Server Error!")})
    @PostMapping(value = "/toEvent/{password}")
    public ResponseTransfer toEvent(@ApiParam(value = "Required event password", required = true)
                                    @PathVariable String password) {
        try {
            eventService.joinToEvent(password);
        } catch (InvalidInputException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong password");
        } catch (ObjectNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event Does Not Exist");
        } catch (UserBelongEventException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already belong to this event");
        }
        return new ResponseTransfer("Successfully joined the event");
    }

    @ApiOperation(value = "Returns event users", authorizations = {@Authorization(value = "authkey")})
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 403, message = "\"Event not found!\" or \"Users for this event not found!\" or \"You are not the administrator of this event!\""),
            @ApiResponse(code = 500, message = "Server Error!")})
    @GetMapping(value = "/getEventUsers/{eventId}")
    public List<UserData> showAllEvents(@ApiParam(value = "Required event ID", required = true)
                                        @PathVariable Long eventId) {
        try {
            return eventService.getEventUsers(eventId);
        } catch (EventNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event not found!");
        } catch (ObjectNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Users for this event not found!");
        } catch (PermissionDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not the administrator of this event!");
        }
    }
}