package fr.makizart.common.storageservice;

import fr.makizart.common.database.repositories.*;
import fr.makizart.common.database.table.*;
import fr.makizart.common.storageservice.dto.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import javax.naming.NameAlreadyBoundException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.regex.Pattern;

@Component
@Transactional
public class SimpleStorageService implements StorageService {


	public static final String FS_SAVE_PATH = "";
	public final ProjectRepository projectRepository;

	public final ArResourceAssetRepository arResourceRepository;

	public final ImageAssetRepository imageAssetRepository;

	public final VideoAssetRepository videoAssetRepository;
	public final SoundAssetReposetory soundAssetReposetory;

	public final MarkerAssetRepository markerAssetRepository;


	final Pattern invalidName = Pattern.compile("[^-_.A-Za-z0-9]");


	public SimpleStorageService(
			@Autowired ProjectRepository projectRepository,
			@Autowired ArResourceAssetRepository arResourceRepository,
			@Autowired MarkerAssetRepository markerAssetRepository,
			@Autowired ImageAssetRepository imageAssetRepository,
			@Autowired VideoAssetRepository videoAssetRepository,
			@Autowired SoundAssetReposetory soundAssetReposetory,
			@Autowired FileSystemManager fileSystemManager) {
		this.projectRepository = projectRepository;
		this.arResourceRepository = arResourceRepository;
		this.imageAssetRepository = imageAssetRepository;
		this.videoAssetRepository = videoAssetRepository;
		this.soundAssetReposetory = soundAssetReposetory;
		this.markerAssetRepository = markerAssetRepository;
    }

	@Override
	public Page<Project> getProjects(int nbPage, int size) {
		return projectRepository.findAll(PageRequest.of(nbPage, size));
	}

	@Override
	public ProjectDTO getProject(String projectId) throws NoSuchElementException, InvalidParameterException {
		return new ProjectDTO(tryGetProject(projectId));
	}

	@Override
	public List<String> getResourcesInProject(String projectId) throws InvalidParameterException, NoSuchElementException {
		return tryGetProject(projectId).getArResource().stream().map(Objects::toString).toList();
	}

	@Override
	public ArResourceDTO getResource(String resourceId) throws InvalidParameterException, NoSuchElementException {
		return new ArResourceDTO(tryGetResource(resourceId));
	}

	@Override
	public StorageInformationDTO getStorageInformation() {
		Map<String,Long> map = FileSystemManager.getDiskSpace();
		return new StorageInformationDTO(map.get("used"),map.get("total"));
	}

	@Override
	@Transactional
	public void overrideMarkers(MarkerDTO markerDTO ) throws InvalidParameterException, NoSuchElementException, IOException, NameAlreadyBoundException {
		// Check 1: Is the name valid?
		validateName(markerDTO.name());

		// Check 2: Do we have 3 markers and are they “marker”+{1-3}
		if(markerDTO.marker1() == null || markerDTO.marker2() == null || markerDTO.marker3() == null){
			throw new InvalidParameterException("3 markers are required (Iset, Fset, Fset3)");
		}
		try {
			FileSystemManager.writeGenericMarkers(markerDTO);
		}catch (IOException e){
			//hide error to end-user
			throw new IOException("Write failed");
		}

	}


	@Override
	@Transactional
	public void overrideSound(String resourceId, String name, byte[] sound) throws InvalidParameterException, NoSuchElementException, IOException, NameAlreadyBoundException {
		validateName(name);
		try {
			AudioFileFormat audio = AudioSystem.getAudioFileFormat(new ByteArrayInputStream(sound));
			if(!soundAssetReposetory.existsById(Long.valueOf(resourceId))){
				throw new  NoSuchElementException();
			}
			SoundAsset soundAsset = new SoundAsset();
			soundAsset.setName(name);

			Path savedPath = FileSystemManager.writeSound(soundAsset.getId().toString(),sound);
			soundAsset.setPathToRessource(savedPath.toUri());
			soundAssetReposetory.save(soundAsset);
		} catch (UnsupportedAudioFileException e) {
			throw new InvalidParameterException("audio type not supported");
		}
	}

	@Override
	public void overrideVideo(String videoId, String name, String url) throws InvalidParameterException, NoSuchElementException, IOException, NameAlreadyBoundException {
		validateName(name);
		try {
			tryGetVideo(videoId).setVideoURL(URI.create(url).toURL());
		}catch (IllegalArgumentException e){
			throw new InvalidParameterException();
		}
	}

	@Override
	public void renameProject(String projectId, String newName) throws InvalidParameterException, NoSuchElementException {
		validateName(newName);
		Project project = tryGetProject(projectId);
		project.setName(newName);
		projectRepository.save(project);

	}

	@Override
	public void renameResource(String resourceId, String newName) throws InvalidParameterException, NoSuchElementException {
		validateName(newName);
		ArResource resource = tryGetResource(resourceId);
		resource.setName(newName);
		arResourceRepository.save(resource);
	}

	@Override
	public void deleteProject(String projectId) throws InvalidParameterException, NoSuchElementException {
		projectRepository.delete(tryGetProject(projectId));
	}

	@Override
	public void deleteResource(String resourceId) throws InvalidParameterException, NoSuchElementException {
		arResourceRepository.delete(tryGetResource(resourceId));
	}

	@Override
	public IdDTO createProject(String name) throws InvalidParameterException, NameAlreadyBoundException {
		validateName(name);
		Project project = new Project(name);
		projectRepository.save(project);
		return new IdDTO(project.getId().toString());
	}

	@Override
	public ArResourceDTO createResource(String projectId, IncomingResourceDTO incomingResourceDTO) throws InvalidParameterException, NameAlreadyBoundException {
		ArResource resource = new ArResource();
		resource.setName(incomingResourceDTO.name());

		boolean atLeastOneAsset = incomingResourceDTO.imageAsset() != null || incomingResourceDTO.soundAsset() != null || incomingResourceDTO.videoAsset() != null;
		boolean noVideoAndSoundOrImage = (incomingResourceDTO.imageAsset() != null || incomingResourceDTO.soundAsset() != null) == (incomingResourceDTO.videoAsset() != null);
		if (noVideoAndSoundOrImage) {
			throw new IllegalStateException("(ImageAsset, VideoAsset) and SoundAsset are mutually exclusive.");
		}
		if (!atLeastOneAsset){
			throw new IllegalStateException("Cannot create resource without asset");
		}

		try {
			ImageAsset thumbnail = new ImageAsset();
			imageAssetRepository.save(thumbnail);//id created
			Path path = FileSystemManager.writeImage(thumbnail.getId().toString(), Base64.getDecoder().decode(incomingResourceDTO.thumbnail()));
			thumbnail.setPathToRessource(path.toUri());
			resource.setThumbnail(thumbnail);
		}catch(IOException e){
			throw new InvalidParameterException("Can't create thumbnail");
		}

		try {
			ARjsMarker markers = new ARjsMarker();
			markerAssetRepository.save(markers);
			MarkerDTO markerDTO = new MarkerDTO(markers.getId(),markers.getId().toString(),incomingResourceDTO.marker1(),incomingResourceDTO.marker2(),incomingResourceDTO.marker3()); //(Long id, String name, String marker1, String marker2, String marker3)
			Map<String,Path> paths = FileSystemManager.writeGenericMarkers(markerDTO);
			markers.setMarker1Path(paths.get("marker1").toUri());
			markers.setMarker2Path(paths.get("marker2").toUri());
			markers.setMarker3Path(paths.get("marker3").toUri());
			resource.setMarkers(markers);
		}catch(IOException e){
			throw new InvalidParameterException("Can't create markers");
		}

		try {
			ImageAsset image = new ImageAsset();
			imageAssetRepository.save(image);//id created
			Path path = FileSystemManager.writeImage(image.getId().toString(), Base64.getDecoder().decode(incomingResourceDTO.imageAsset()));
			image.setPathToRessource(path.toUri());
			resource.setImageAsset(image);
		}catch(IOException e){
			throw new InvalidParameterException("Can't create image");
		}

		try {
			SoundAsset sound = new SoundAsset();
			soundAssetReposetory.save(sound);//id created
			Path path = FileSystemManager.writeSound(sound.getId().toString(), Base64.getDecoder().decode(incomingResourceDTO.soundAsset()));
			sound.setPathToRessource(path.toUri());
			resource.setSoundAsset(sound);
		}catch(IOException e){
			throw new InvalidParameterException("Can't create sound");
		}

		try {
			VideoAsset video = new VideoAsset();
			videoAssetRepository.save(video);//id created
			video.setVideoURL(URI.create(incomingResourceDTO.videoAsset()).toURL());
			resource.setVideoAsset(video);
		}catch(IOException e){
			throw new InvalidParameterException("Can't create video");
		}

		return new ArResourceDTO(resource);
	}

	@Override
	public void overrideImage(String resourceId, String name, String image) throws InvalidParameterException, IOException, NameAlreadyBoundException {
		validateName(resourceId);

		ImageAsset imageAsset = tryGetImage(image);

		Path path = FileSystemManager.writeImage(name,Base64.getDecoder().decode(image));
		imageAsset.setPathToRessource(path.toUri());
		imageAssetRepository.save(imageAsset);
	}


	private void validateName(String newName) {
		if(invalidName.matcher(newName).find())
			throw new InvalidParameterException();
	}

	private ImageAsset tryGetImage(String imageId) {
		try {
			return imageAssetRepository.getReferenceById(Long.valueOf(imageId));
		}catch (NumberFormatException e){
			throw new InvalidParameterException();
		}catch (EntityNotFoundException e){
			throw new NoSuchElementException();
		}
	}

	private Project tryGetProject(String projectId) {
		try {
			return projectRepository.getReferenceById(Long.valueOf(projectId));
		}catch (NumberFormatException e){
			throw new InvalidParameterException();
		}catch (EntityNotFoundException e){
			throw new NoSuchElementException();
		}
	}
	private ArResource tryGetResource(String resourceID) {
		try {
			return arResourceRepository.getReferenceById(Long.valueOf(resourceID));
		}catch (NumberFormatException e){
			throw new InvalidParameterException();
		}catch (EntityNotFoundException e){
			throw new NoSuchElementException();
		}
	}
	private VideoAsset tryGetVideo(String resourceID) {
		try {
			return videoAssetRepository.getReferenceById(Long.valueOf(resourceID));
		}catch (NumberFormatException e){
			throw new InvalidParameterException();
		}catch (EntityNotFoundException e){
			throw new NoSuchElementException();
		}
	}

}
