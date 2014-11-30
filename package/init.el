;;; package/init --- loads alternative package repositories
;;;; Commentary:
;;; Code:
(require 'package)
(add-to-list 'package-archives
             '("marmalade" . "http://marmalade-repo.org/packages/") t)
(add-to-list 'package-archives
                          '("melpa" . "http://melpa.org/packages/") t)

(package-initialize)
(provide 'init)

;;; init.el ends here
