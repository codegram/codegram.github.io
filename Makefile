all: build

build:
	lein build-site

deploy:
	rm -fR /tmp/blog && mkdir -p /tmp/blog
	cp -r output/* /tmp/blog
	git checkout master
	rm -fR *
	cp -r /tmp/blog/* .
	git add --all
	git commit -m "Update blog"
	git push -f origin master
	git checkout source

.PHONY: build deploy
