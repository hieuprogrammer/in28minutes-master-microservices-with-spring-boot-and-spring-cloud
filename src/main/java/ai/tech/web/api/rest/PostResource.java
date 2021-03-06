package ai.tech.web.api.rest;

import ai.tech.domain.Post;
import ai.tech.domain.User;
import ai.tech.service.PostService;
import ai.tech.web.exception.NotFoundException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/posts")
public class PostResource {
  private final PostService postService;

  @PostMapping
  public ResponseEntity<URI> add(final @Valid @RequestBody Post post) {
    final Optional<Post> savedPost = postService.save(post);
    final URI locationUri =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("api/posts/{uuid}")
            .buildAndExpand(savedPost.get().getUuid())
            .toUri();

    return ResponseEntity.created(locationUri).build();
  }

  private FilterProvider filterPosts() {
    final SimpleBeanPropertyFilter simpleBeanPropertyFilter =
        SimpleBeanPropertyFilter.filterOutAllExcept("uuid", "title", "summary", "content", "user");
    final FilterProvider filterProvider =
        new SimpleFilterProvider().addFilter("PostFilter", simpleBeanPropertyFilter);

    return filterProvider;
  }

  @GetMapping
  public ResponseEntity<MappingJacksonValue> getAll() {
    final List<Post> postsList = this.postService.findAll().get();
    final FilterProvider filterProvider = this.filterPosts();
    final MappingJacksonValue mappingJacksonValue = new MappingJacksonValue(postsList);
    mappingJacksonValue.setFilters(filterProvider);

    return ResponseEntity.ok(mappingJacksonValue);
  }

  @GetMapping("/{uuid}")
  public ResponseEntity<MappingJacksonValue> getByUuid(final @PathVariable("uuid") UUID uuid) {
    final Optional<Post> foundPostOptional = this.postService.findById(uuid);
    if (foundPostOptional.isEmpty())
      throw new NotFoundException("Post with UUID: " + uuid + " is not found.");

    final EntityModel foundUserEntityModel = EntityModel.of(foundPostOptional.get());
    final WebMvcLinkBuilder linkToUsers =
        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getAll());
    foundUserEntityModel.add(linkToUsers.withRel("all-posts"));

    final FilterProvider filterProvider = this.filterPosts();
    final MappingJacksonValue mappingJacksonValue = new MappingJacksonValue(foundUserEntityModel);
    mappingJacksonValue.setFilters(filterProvider);

    return ResponseEntity.ok(mappingJacksonValue);
  }

  @GetMapping("/{uuid}/owner")
  public ResponseEntity<MappingJacksonValue> getUserByPostUuid(final @PathVariable("uuid") UUID uuid) {
    final Optional<Post> foundPostOptional = this.postService.findById(uuid);
    final Optional<User> foundUserOptional = Optional.of(foundPostOptional.get().getUser());
    if (foundPostOptional.isEmpty())
      throw new NotFoundException("Post with UUID: " + uuid + " is not found.");

    final EntityModel foundUserEntityModel = EntityModel.of(foundUserOptional.get());
    final WebMvcLinkBuilder linkToUsers =
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getAll());
    foundUserEntityModel.add(linkToUsers.withRel("all-posts"));

    final FilterProvider filterProvider = this.filterPosts();
    final MappingJacksonValue mappingJacksonValue = new MappingJacksonValue(foundUserEntityModel);
    mappingJacksonValue.setFilters(filterProvider);

    return ResponseEntity.ok(mappingJacksonValue);
  }

  @DeleteMapping("/{uuid}")
  public ResponseEntity<Void> deleteById(final @PathVariable("uuid") UUID uuid) {
    if (this.postService.findById(uuid).isEmpty())
      throw new NotFoundException("Post with UUID: " + uuid + " does not exist.");

    this.postService.deleteById(uuid);
    return ResponseEntity.noContent().build();
  }
}
