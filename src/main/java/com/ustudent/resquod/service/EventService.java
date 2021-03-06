package com.ustudent.resquod.service;

import com.ustudent.resquod.exception.*;
import com.ustudent.resquod.model.*;
import com.ustudent.resquod.model.dao.*;
import com.ustudent.resquod.repository.EventRepository;
import com.ustudent.resquod.repository.UserRepository;
import com.ustudent.resquod.validator.EventValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EventService {

    private final EventValidator eventValidator;
    private final EventRepository eventRepository;
    private final RoomService roomService;
    private final UserService userService;
    private final CorporationService corporationService;
    private final AttendanceListService attendanceListService;
    private final PresenceService presenceService;
    private final UserRepository userRepository;

    @Autowired
    public EventService(EventValidator eventValidator,
                        EventRepository eventRepository,
                        RoomService roomService,
                        UserService userService,
                        CorporationService corporationService,
                        AttendanceListService attendanceListService,
                        PresenceService presenceService,
                        UserRepository userRepository) {
        this.eventValidator = eventValidator;
        this.eventRepository = eventRepository;
        this.roomService = roomService;
        this.userService = userService;
        this.corporationService = corporationService;
        this.attendanceListService = attendanceListService;
        this.presenceService = presenceService;
        this.userRepository = userRepository;
    }

    public void addNewEvent(NewEventData newEvent) throws EventAlreadyExistsException, PermissionDeniedException {

        User admin = userService.getLoggedUser();
        Corporation corporation = roomService.getRoomById(newEvent.getRoomId()).getCorporation();

        if (!(admin.getRole().equals("ROLE_ADMIN") ||
                (admin.getRole().equals("ROLE_OWNER") && admin.getCorporations().contains(corporation))))
            throw new PermissionDeniedException();

        if (!checkIfEventExists(newEvent)) {
            eventValidator.validateEvent(newEvent);
            Event event = new Event();
            event.setAdministratorId(admin.getId());
            Room room = roomService.getRoomById(newEvent.getRoomId());
            event.setName(newEvent.getName());
            event.setPassword(newEvent.getPassword());
            event.setRoom(room);
            eventRepository.save(event);
        } else throw new EventAlreadyExistsException();
    }

    private boolean checkIfEventExists(NewEventData newEvent) {
        return eventRepository.findByName(newEvent.getName(), newEvent.getRoomId()).isPresent();
    }

    public Set<EventDTO> findEventsWhereUserIsAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        User user = userService.getUserByEmail(email);
        Set<Event> events = eventRepository.findByAdministratorId(user.getId());
        return getEventDTO(events);
    }

    private Set<EventDTO> getEventDTO(Set<Event> events) {
        Set<EventDTO> eventsU = new HashSet<>();
        for (Event e : events) {
            CorporationDTO corporation = new CorporationDTO(e.getRoom().getCorporation().getId(), e.getRoom().getCorporation().getName());
            RoomDTO room = new RoomDTO(e.getRoom().getId(), e.getRoom().getName(), corporation);
            eventsU.add(new EventDTO(e.getId(), e.getName(), e.getAdministratorId(), e.getPassword(), room));
        }
        return eventsU;
    }

    private List<EventDTO> getEventDTO(List<Event> events) {
        List<EventDTO> eventsU = new LinkedList<>();
        for (Event e : events) {
            CorporationDTO corporation = new CorporationDTO(e.getRoom().getCorporation().getId(), e.getRoom().getCorporation().getName());
            RoomDTO room = new RoomDTO(e.getRoom().getId(), e.getRoom().getName(), corporation);
            eventsU.add(new EventDTO(e.getId(), e.getName(), e.getAdministratorId(), e.getPassword(), room));
        }
        return eventsU;
    }

    public Set<EventDTO> findUserEvents() {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        User user = userService.getUserByEmail(email);
        Set<Event> events = user.getEvents();
        return getEventDTO(events);
    }

    public List<EventDTO> showEveryEvent() {
        List<Event> events = eventRepository.findAll();
        return getEventDTO(events);
    }

    public void changeEventData(EventData inputData) throws InvalidInputException, ObjectNotFoundException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        User user = userService.getUserByEmail(email);
        Event event = eventRepository.findById(inputData.getId()).orElseThrow(ObjectNotFoundException::new);
        Room room = roomService.findById(inputData.getRoomId());
        Corporation corpoId = event.getRoom().getCorporation();
        if (!(user.getId().equals(event.getAdministratorId()) || (user.getRole().equals("ROLE_OWNER") && user.getCorporations().contains(corpoId)) || user.getRole().equals("ROLE_ADMIN")))
            throw new InvalidAdminId();
        if (inputData.getName() == null || inputData.getName().length() < 2
                || inputData.getRoomId() == null)
            throw new InvalidInputException();
        event.setName(inputData.getName());
        if (eventRepository.findByPassword(inputData.getPassword()).isPresent())
            throw new PasswordAlreadyExists();
        event.setPassword(inputData.getPassword());
        if (!(event.getRoom().getCorporation().getId().equals(room.getCorporation().getId())))
            throw new RoomDoesntBelongToYourCorpo();
        event.setRoom(room);
        eventRepository.save(event);

    }

    public List<EventData> findByCorpoId(Long corpoId) throws PermissionDeniedException {

        User admin = userService.getLoggedUser();
        Corporation corporation = corporationService.getCorpoById(corpoId);

        if (!(admin.getRole().equals("ROLE_ADMIN") ||
                (admin.getRole().equals("ROLE_OWNER") && admin.getCorporations().contains(corporation))))
            throw new PermissionDeniedException();

        List<Event> events = eventRepository.findByCorpoId(corpoId);
        List<EventData> corpoEvents = new ArrayList<>();
        for (Event e : events) {
            corpoEvents.add(new EventData(e.getId(), e.getName(), e.getRoom().getId(), e.getPassword()));
        }
        return corpoEvents;
    }


    public void joinToEvent(String password) throws InvalidInputException, ObjectNotFoundException, UserBelongEventException {
        if (password == null) {
            throw new InvalidInputException();
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        User user = userService.getUserByEmail(email);
        Event event = eventRepository.findByPassword(password).orElseThrow(ObjectNotFoundException::new);
        Set<User> users = event.getUsers();
        if(users.contains(user)){
            throw new UserBelongEventException();
        }
        users.add(user);
        event.setUsers(users);
        eventRepository.save(event);
        List<AttendanceList> listOfAttendanceList = attendanceListService.getAttendanceList(event.getId());
        for (AttendanceList attendanceList : listOfAttendanceList) {
            presenceService.createPresence(user.getId(), attendanceList.getId());
        }
    }

    public List<UserData> getEventUsers(Long eventId) throws EventNotFoundException, PermissionDeniedException, ObjectNotFoundException{
        User user = userService.getLoggedUser();
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
        if(!(user.getId() == event.getAdministratorId() || user.getRole().equals("ROLE_ADMIN") || (user.getRole().equals("ROLE_OWNER") && user.getCorporations().contains(event.getRoom().getCorporation())))){
            throw new PermissionDeniedException();
        }
        List<UserData> users = userRepository.findUserDataByEventId(eventId);
        if(users.isEmpty()){
            throw new ObjectNotFoundException();
        }
        return users;
    }
}
