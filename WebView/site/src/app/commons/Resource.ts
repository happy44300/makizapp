/**
 * @class Resource
 * Represents an resource with various media attributes.
 */
export class Resource {

  /**
   * @property id
   * Represents the id of the resource.
   */
  public readonly id: string;

  /**
   * @property accessCount
   * Represents the number of times the resource has been accessed.
   */
  public readonly accessCount: number = 0;

  /**
   * @property name
   * Represents the name of the resource.
   */
  public name: string;

  /**
   * @property createdOn
   * Represents the creation date of the resource.
   */
  public readonly createdOn: string;

  /**
   * @property thumbnailId
   * Represents the id of the media where stocked tracked image of the resource.
   */
  public thumbnailId: string;

  /**
   * @property videoAssetId
   * The video media ID of this resource.
   */
  public videoAssetId: string;

  /**
   * @property imageAssetId
   * The image media id of this resource.
   */
  public imageAssetId: string;

  /**
   * @property soundAssetId
   * The audio media id of this resource.
   */
  public soundAssetId: string;

  /**
   * @property thumbnail
   * Represents the tracked image of the resource in base string.
   */
  public thumbnail: string;

  /**
   * @property videoAsset
   * Represents the video in url form.
   */
  public videoAsset: string;

  /**
   * @property imageAsset
   * Represents the image to display, it is in base64 form.
   */
  public imageAsset: string;

  /**
   * @property soundAsset
   * Represents the audio to listen to, it is in base64 form.
   */
  public soundAsset: string;

  /**
   * @property thumbnail_size
   * Represents the size of the tracked image of the ressource.
   */
  public thumbnail_size: number = 0;

  /**
   * @property image_size
   * Represents the size of the image of the ressource.
   */
  public image_size: number = 0;

  /**
   * @property audio_size
   * Represents the size of the audio of the ressource.
   */
  public audio_size: number = 0;

  /**
   * @property markers_size
   * Represents the size of the markers of the ressource.
   */
  public markers_size: number = 0;

  /**
   * @constructor
   * Creates a new instance of the Resource.
   */

  constructor(id: string, accessCount: number, name: string, createdOn: string, thumbnailId: string, videoAssetId: string, imageAssetId: string, soundAssetId: string, thumbnail: string, videoAsset: string, imageAsset: string, soundAsset: string, thumbnail_size: number, image_size: number, audio_size: number, markers_size: number) {
    this.id = id;
    this.accessCount = accessCount;
    this.name = name;
    this.createdOn = createdOn;
    this.thumbnailId = thumbnailId;
    this.videoAssetId = videoAssetId;
    this.imageAssetId = imageAssetId;
    this.soundAssetId = soundAssetId;
    this.thumbnail = thumbnail;
    this.videoAsset = videoAsset;
    this.imageAsset = imageAsset;
    this.soundAsset = soundAsset;
    this.thumbnail_size = thumbnail_size;
    this.image_size = image_size;
    this.audio_size = audio_size;
    this.markers_size = markers_size;
  }

}
